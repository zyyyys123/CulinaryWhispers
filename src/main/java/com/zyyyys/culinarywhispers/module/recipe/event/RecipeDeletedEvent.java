package com.zyyyys.culinarywhispers.module.recipe.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 食谱删除事件
 * @author zyyyys
 */
@Getter
public class RecipeDeletedEvent extends ApplicationEvent {

    private final Long recipeId;

    public RecipeDeletedEvent(Object source, Long recipeId) {
        super(source);
        this.recipeId = recipeId;
    }
}
