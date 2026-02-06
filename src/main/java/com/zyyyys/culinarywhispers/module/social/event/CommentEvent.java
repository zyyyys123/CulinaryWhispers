package com.zyyyys.culinarywhispers.module.social.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 评论发布事件
 * @author zyyyys
 */
@Getter
public class CommentEvent extends ApplicationEvent {
    
    private final Long recipeId;
    private final boolean isAdd; // true: add, false: delete

    public CommentEvent(Object source, Long recipeId, boolean isAdd) {
        super(source);
        this.recipeId = recipeId;
        this.isAdd = isAdd;
    }
}
