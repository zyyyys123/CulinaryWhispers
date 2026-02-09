package com.zyyyys.culinarywhispers.module.user.listener;

import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.user.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户成长体系监听器
 * @author zyyyys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGrowthListener {

    private final UserStatsService userStatsService;

    /**
     * 监听食谱发布事件
     * 奖励: 经验值 +50, 发布数 +1
     */
    @Async
    @EventListener
    public void onRecipePublished(RecipePublishedEvent event) {
        Long userId = event.getAuthorId();
        log.info("Processing user growth for recipe published. User: {}", userId);
        
        try {
            // 1. 增加发布计数
            userStatsService.incrementRecipeCount(userId);
            
            // 2. 增加经验值 (假设发布食谱 +50 EXP)
            userStatsService.addExperience(userId, 50);
        } catch (Exception e) {
            log.error("Failed to update user growth stats for user: " + userId, e);
        }
    }
}
