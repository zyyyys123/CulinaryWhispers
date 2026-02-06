package com.zyyyys.culinarywhispers.module.recipe.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTag;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTagRelation;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeDeletedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipePublishedEvent;
import com.zyyyys.culinarywhispers.module.recipe.event.RecipeUpdatedEvent;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagMapper;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeTagRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 食谱标签监听器
 * 负责处理标签的创建、计数更新及关联
 * @author zyyyys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeTagListener {

    private final RecipeTagMapper tagMapper;
    private final RecipeTagRelationMapper tagRelationMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRecipePublished(RecipePublishedEvent event) {
        log.info("Processing tags for recipe: {}", event.getRecipeId());
        processTags(event.getRecipeId(), event.getTags());
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRecipeUpdated(RecipeUpdatedEvent event) {
        log.info("Updating tags for recipe: {}", event.getRecipeId());
        
        // 1. 删除旧关联
        LambdaQueryWrapper<RecipeTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecipeTagRelation::getRecipeId, event.getRecipeId());
        tagRelationMapper.delete(wrapper);

        // 2. 重新处理标签 (逻辑复用)
        // 构造一个临时的 PublishedEvent 复用逻辑，或者抽取公共方法
        // 这里为了简单直观，直接调用 handleRecipePublished 的逻辑，但需要适配参数
        // 更好的方式是抽取 processTags 方法
        processTags(event.getRecipeId(), event.getTags());
    }

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleRecipeDeleted(RecipeDeletedEvent event) {
        log.info("Deleting tag relations for recipe: {}", event.getRecipeId());
        // 删除关联关系
        LambdaQueryWrapper<RecipeTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecipeTagRelation::getRecipeId, event.getRecipeId());
        tagRelationMapper.delete(wrapper);
        // 注意：这里暂不处理 t_rcp_tag 的 use_count 递减，因为比较复杂且不是核心路径
    }

    private void processTags(Long recipeId, java.util.List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String tagName : tags) {
            if (!StringUtils.hasText(tagName)) continue;

            // 查找或创建标签
            LambdaQueryWrapper<RecipeTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(RecipeTag::getName, tagName);
            RecipeTag tag = tagMapper.selectOne(tagWrapper);

            if (tag == null) {
                tag = new RecipeTag();
                tag.setName(tagName);
                tag.setType(1); // 默认为通用
                tag.setUseCount(1);
                tagMapper.insert(tag);
            } else {
                tag.setUseCount(tag.getUseCount() + 1);
                tagMapper.updateById(tag);
            }

            // 创建关联
            RecipeTagRelation relation = new RecipeTagRelation();
            relation.setRecipeId(recipeId);
            relation.setTagId(tag.getId());
            tagRelationMapper.insert(relation);
        }
    }
}
