package com.zyyyys.culinarywhispers.module.recipe.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 食谱更新事件
 * @author zyyyys
 */
@Getter
public class RecipeUpdatedEvent extends ApplicationEvent {

    private final Long recipeId;
    private final List<String> tags;

    public RecipeUpdatedEvent(Object source, Long recipeId, List<String> tags) {
        super(source);
        this.recipeId = recipeId;
        this.tags = tags;
    }
}
