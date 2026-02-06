package com.zyyyys.culinarywhispers.module.recipe.event;

import com.zyyyys.culinarywhispers.module.recipe.dto.RecipePublishDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 食谱发布事件
 * 用于解耦食谱发布后的副作用逻辑（如统计初始化、标签处理、消息通知等）
 * @author zyyyys
 */
@Getter
public class RecipePublishedEvent extends ApplicationEvent {

    private final Long recipeId;
    private final Long authorId;
    private final List<String> tags;

    public RecipePublishedEvent(Object source, Long recipeId, Long authorId, List<String> tags) {
        super(source);
        this.recipeId = recipeId;
        this.authorId = authorId;
        this.tags = tags;
    }
}
