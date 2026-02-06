package com.zyyyys.culinarywhispers.module.recipe.listener;

import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 食谱统计监听器
 * 负责初始化和清理食谱统计数据
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
}
