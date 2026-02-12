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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleRecipePublished(RecipePublishedEvent event) {
        log.info("Processing tags for recipe: {}", event.getRecipeId());
        try {
            processTags(event.getRecipeId(), event.getTags());
        } catch (Exception e) {
            // 标签属于“辅助能力”，不应影响主链路（发布菜谱）结果
            log.error("Failed to process tags for recipe: {}", event.getRecipeId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleRecipeUpdated(RecipeUpdatedEvent event) {
        log.info("Updating tags for recipe: {}", event.getRecipeId());

        try {
            // 1. 删除旧关联
            LambdaQueryWrapper<RecipeTagRelation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecipeTagRelation::getRecipeId, event.getRecipeId());
            tagRelationMapper.delete(wrapper);

            // 2. 重新处理标签
            processTags(event.getRecipeId(), event.getTags());
        } catch (Exception e) {
            // 标签属于“辅助能力”，不应影响主链路（更新菜谱）结果
            log.error("Failed to update tags for recipe: {}", event.getRecipeId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleRecipeDeleted(RecipeDeletedEvent event) {
        log.info("Deleting tag relations for recipe: {}", event.getRecipeId());
        try {
            // 删除关联关系
            LambdaQueryWrapper<RecipeTagRelation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecipeTagRelation::getRecipeId, event.getRecipeId());
            tagRelationMapper.delete(wrapper);
            // 注意：这里暂不处理 t_rcp_tag 的 use_count 递减，因为比较复杂且不是核心路径
        } catch (Exception e) {
            log.error("Failed to delete tag relations for recipe: {}", event.getRecipeId(), e);
        }
    }

    private void processTags(Long recipeId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        // 去重：避免同一个菜谱在一次发布/更新中出现重复标签，导致唯一索引冲突引发“系统繁忙”
        LinkedHashSet<String> distinctNames = new LinkedHashSet<>();
        for (String raw : tags) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String trimmed = raw.trim();
            if (StringUtils.hasText(trimmed)) {
                distinctNames.add(trimmed);
            }
        }
        if (distinctNames.isEmpty()) {
            return;
        }

        for (String tagName : distinctNames) {
            // 查找或创建标签
            LambdaQueryWrapper<RecipeTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(RecipeTag::getName, tagName);
            RecipeTag tag = tagMapper.selectOne(tagWrapper);

            if (tag == null) {
                tag = new RecipeTag();
                tag.setName(tagName);
                tag.setType(1); // 默认为通用
                tag.setUseCount(1);
                try {
                    tagMapper.insert(tag);
                } catch (DuplicateKeyException duplicateKeyException) {
                    // 并发场景下，可能另一个线程先插入了同名标签；这里兜底重查一次即可
                    tag = tagMapper.selectOne(tagWrapper);
                    if (tag == null) {
                        throw duplicateKeyException;
                    }
                }
            } else {
                tag.setUseCount(tag.getUseCount() + 1);
                tagMapper.updateById(tag);
            }

            // 创建关联
            RecipeTagRelation relation = new RecipeTagRelation();
            relation.setRecipeId(recipeId);
            relation.setTagId(tag.getId());
            try {
                tagRelationMapper.insert(relation);
            } catch (DuplicateKeyException duplicateKeyException) {
                // 已有关联则忽略：避免影响主链路
            }
        }
    }
}
