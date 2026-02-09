package com.zyyyys.culinarywhispers.module.recipe.task;

import com.zyyyys.culinarywhispers.common.constant.RedisKeyConstant;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Set;

/**
 * 食谱统计数据同步任务
 * 定时将 Redis 中的统计数据同步回 MySQL (Write-Back)
 * @author zyyyys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeStatsSyncTask {

    private final StringRedisTemplate redisTemplate;
    private final RecipeStatsMapper statsMapper;

    /**
     * 每 1 分钟同步一次
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void syncStatsToDb() {
        log.info("Starting recipe stats sync task...");

        // 1. 获取所有脏数据 ID
        Set<String> dirtyIds = redisTemplate.opsForSet().members(RedisKeyConstant.RECIPE_STATS_DIRTY_SET);
        if (CollectionUtils.isEmpty(dirtyIds)) {
            log.info("No dirty stats to sync.");
            return;
        }

        int count = 0;
        for (String idStr : dirtyIds) {
            Long recipeId = Long.valueOf(idStr);
            String key = RedisKeyConstant.RECIPE_STATS_PREFIX + recipeId;
            
            // 2. 获取 Redis 中的统计数据
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (entries.isEmpty()) {
                continue;
            }

            try {
                // 3. 组装对象
                RecipeStats stats = new RecipeStats();
                stats.setRecipeId(recipeId);
                
                // Parse safely
                stats.setViewCount(parseLong(entries.get("view_count")));
                stats.setLikeCount(parseLong(entries.get("like_count")));
                stats.setCollectCount(parseLong(entries.get("collect_count")));
                stats.setCommentCount(parseLong(entries.get("comment_count")));
                stats.setShareCount(parseLong(entries.get("share_count")));
                
                // 4. 更新 DB (这里使用 Selective Update 更好，但为了简单直接覆盖相关字段)
                // 注意: 这种覆盖可能会覆盖掉 "try_count" 或 "score" 如果它们不在 Redis 中且我们创建了新对象
                // 所以应该先查 DB，或者确保 SQL 只更新特定字段
                // MyBatis-Plus updateById 是动态 SQL，只更新非 null 字段
                // 但我们 new 了一个对象，除了设置的字段外都是 null，这正好符合 updateById 的逻辑 (只更新设置的字段)
                // 前提是 id 必须存在。
                
                // 但是 try_count 和 score 我们没设置，它们是 null，不会被更新，这是对的。
                // 唯独 recipeId 是主键。
                
                statsMapper.updateById(stats);
                
                // 5. 从脏集合中移除
                redisTemplate.opsForSet().remove(RedisKeyConstant.RECIPE_STATS_DIRTY_SET, idStr);
                count++;
            } catch (Exception e) {
                log.error("Failed to sync stats for recipe: " + recipeId, e);
            }
        }

        log.info("Synced {} recipe stats to DB.", count);
    }

    private Long parseLong(Object val) {
        if (val == null) return null;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
