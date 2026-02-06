package com.zyyyys.culinarywhispers.module.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 互动实体类
 * 记录点赞、收藏、分享等行为
 * @author zyyyys
 */
@Data
@TableName("t_soc_interaction")
public class Interaction implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标类型: 1-食谱, 2-评论, 3-动态
     */
    private Integer targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 动作类型: 1-点赞, 2-收藏, 3-分享
     */
    private Integer actionType;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;
}
