package com.zyyyys.culinarywhispers.module.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_notify")
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private Integer type;

    private Integer targetType;

    private Long targetId;

    private String content;

    private Integer isRead;

    private LocalDateTime gmtCreate;
}
