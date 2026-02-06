package com.zyyyys.culinarywhispers.module.recipe.listener;

import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import com.zyyyys.culinarywhispers.module.social.event.CommentEvent;
import com.zyyyys.culinarywhispers.module.social.event.InteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 食谱统计监听器
 * 负责初始化和清理食谱统计数据，以及处理互动事件更新统计
 * @author zyyyys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeStatsListener {

    private final RecipeStatsMapper statsMapper;

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
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRecipeDeleted(RecipeDeletedEvent event) {
        log.info("Deleting stats for recipe: {}", event.getRecipeId());
        statsMapper.deleteById(event.getRecipeId());
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleInteraction(InteractionEvent event) {
        // 仅处理针对食谱 (TargetType=1) 的互动
        if (event.getTargetType() != 1) {
            return;
        }

        log.info("Updating stats for recipe: {}, action: {}, isAdd: {}", 
                event.getTargetId(), event.getActionType(), event.isAdd());

        RecipeStats stats = statsMapper.selectById(event.getTargetId());
        if (stats == null) {
            log.warn("Recipe stats not found for id: {}", event.getTargetId());
            return;
        }

        // 1-点赞, 2-收藏, 3-分享
        long delta = event.isAdd() ? 1 : -1;
        
        switch (event.getActionType()) {
            case 1: // Like
                long newLikeCount = stats.getLikeCount() + delta;
                stats.setLikeCount(newLikeCount < 0 ? 0 : newLikeCount);
                break;
            case 2: // Collect
                long newCollectCount = stats.getCollectCount() + delta;
                stats.setCollectCount(newCollectCount < 0 ? 0 : newCollectCount);
                break;
            case 3: // Share
                if (event.isAdd()) { // Share usually only adds up
                    stats.setShareCount(stats.getShareCount() + 1);
                }
                break;
            default:
                break;
        }
        
        statsMapper.updateById(stats);
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleComment(CommentEvent event) {
        log.info("Updating comment count for recipe: {}, isAdd: {}", event.getRecipeId(), event.isAdd());
        
        RecipeStats stats = statsMapper.selectById(event.getRecipeId());
        if (stats == null) {
            return;
        }
        
        long delta = event.isAdd() ? 1 : -1;
        long newCount = stats.getCommentCount() + delta;
        stats.setCommentCount(newCount < 0 ? 0 : newCount);
        
        statsMapper.updateById(stats);
    }
}
