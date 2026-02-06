package com.zyyyys.culinarywhispers.module.social.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 互动事件
 * 当用户点赞、收藏时发布，用于更新统计数据
 * @author zyyyys
 */
@Getter
public class InteractionEvent extends ApplicationEvent {
    
    private final Long userId;
    private final Integer targetType;
    private final Long targetId;
    private final Integer actionType;
    /**
     * true: 增加互动 (如点赞)
     * false: 取消互动 (如取消点赞)
     */
    private final boolean isAdd;

    public InteractionEvent(Object source, Long userId, Integer targetType, Long targetId, Integer actionType, boolean isAdd) {
        super(source);
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actionType = actionType;
        this.isAdd = isAdd;
    }
}
