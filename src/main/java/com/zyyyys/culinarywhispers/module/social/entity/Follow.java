package com.zyyyys.culinarywhispers.module.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 关注实体类
 * 记录用户间的关注关系
 * @author zyyyys
 */
@Data
@TableName("t_soc_follow")
public class Follow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 粉丝ID
     */
    private Long followerId;

    /**
     * 被关注者ID
     */
    private Long followingId;

    private String remarkName;

    /**
     * 关注状态: 1-关注, 0-取消
     */
    private Integer status;

    /**
     * 关注时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    private LocalDateTime gmtModified;
}
