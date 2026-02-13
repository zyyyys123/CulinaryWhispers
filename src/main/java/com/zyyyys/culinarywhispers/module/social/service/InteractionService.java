package com.zyyyys.culinarywhispers.module.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;
import com.zyyyys.culinarywhispers.module.social.entity.Interaction;
import com.zyyyys.culinarywhispers.module.social.vo.InteractionStatusVO;

/**
 * 互动服务接口
 * @author zyyyys
 */
public interface InteractionService extends IService<Interaction> {

    /**
     * 切换互动状态 (点赞/取消点赞, 收藏/取消收藏)
     * @param userId 用户ID
     * @param targetType 目标类型 (1-食谱, 2-评论, 3-动态)
     * @param targetId 目标ID
     * @param actionType 动作类型 (1-点赞, 2-收藏, 3-分享)
     */
    void toggleInteraction(Long userId, Integer targetType, Long targetId, Integer actionType);

    InteractionStatusVO getStatus(Long userId, Integer targetType, Long targetId);

    Page<RecipePageVO> pageCollectedRecipes(Long userId, int page, int size);
}
