package com.zyyyys.culinarywhispers.module.recipe.listener;

import com.zyyyys.culinarywhispers.common.constant.RedisKeyConstant;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.event.InteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 食谱统计监听器
 * 负责初始化和清理食谱统计数据，以及处理互动事件更新统计
 * 升级: 使用 Redis Write-Back 模式处理高并发计数
 * @author zyyyys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeStatsListener {

    private final RecipeStatsMapper statsMapper;
    private final StringRedisTemplate redisTemplate;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRecipePublished(RecipePublishedEvent event) {
        log.info("Initializing stats for recipe: {}", event.getRecipeId());
        
        RecipeStats stats = new RecipeStats();
        stats.setRecipeId(event.getRecipeId());
        stats.setViewCount(0L);
        stats.setLikeCount(0L);
        stats.setCollectCount(0L);
        stats.setCommentCount(0L);
        stats.setShareCount(0L);
        stats.setTryCount(0);
        stats.setScore(BigDecimal.ZERO);
        
        statsMapper.insert(stats);
        
        // Init Redis
        String key = RedisKeyConstant.RECIPE_STATS_PREFIX + event.getRecipeId();
        redisTemplate.opsForHash().putAll(key, Map.of(
                "view_count", "0",
                "like_count", "0",
                "collect_count", "0",
                "comment_count", "0",
                "share_count", "0"
        ));
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRecipeDeleted(RecipeDeletedEvent event) {
        log.info("Deleting stats for recipe: {}", event.getRecipeId());
        statsMapper.deleteById(event.getRecipeId());
        redisTemplate.delete(RedisKeyConstant.RECIPE_STATS_PREFIX + event.getRecipeId());
    }

    @EventListener
    public void handleInteraction(InteractionEvent event) {
        // 仅处理针对食谱 (TargetType=1) 的互动
        if (event.getTargetType() != 1) {
            return;
        }

        log.info("Updating Redis stats for recipe: {}, action: {}, isAdd: {}", 
                event.getTargetId(), event.getActionType(), event.isAdd());

        String key = RedisKeyConstant.RECIPE_STATS_PREFIX + event.getTargetId();
        long delta = event.isAdd() ? 1 : -1;
        String field = null;

        switch (event.getActionType()) {
            case 1: // Like
                field = "like_count";
                break;
            case 2: // Collect
                field = "collect_count";
                break;
            case 3: // Share
                if (event.isAdd()) {
                    field = "share_count";
                }
                break;
            default:
                break;
        }
        
        if (field != null) {
            incrementRedisStats(event.getTargetId(), key, field, delta);
        }
    }

    @EventListener
    public void handleComment(CommentEvent event) {
        log.info("Updating Redis comment count for recipe: {}, isAdd: {}", event.getRecipeId(), event.isAdd());
        
        String key = RedisKeyConstant.RECIPE_STATS_PREFIX + event.getRecipeId();
        long delta = event.isAdd() ? 1 : -1;
        
        incrementRedisStats(event.getRecipeId(), key, "comment_count", delta);
    }
    
    private void incrementRedisStats(Long recipeId, String key, String field, long delta) {
        // 1. Increment Hash
        redisTemplate.opsForHash().increment(key, field, delta);
        
        // 2. Add to Dirty Set for Async Sync
        redisTemplate.opsForSet().add(RedisKeyConstant.RECIPE_STATS_DIRTY_SET, String.valueOf(recipeId));
    }
}
