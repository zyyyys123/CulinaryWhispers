package com.zyyyys.culinarywhispers.module.notify.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private Integer type;
    private Integer targetType;
    private Long targetId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;

    private Long fromUserId;
    private String fromNickname;
    private String fromAvatarUrl;
}

