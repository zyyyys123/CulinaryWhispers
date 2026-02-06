package com.zyyyys.culinarywhispers.module.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体类
 * 记录用户对食谱的评论及回复
 * @author zyyyys
 */
@Data
@TableName("t_soc_comment")
public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评论者ID
     */
    private Long userId;

    /**
     * 关联食谱ID
     */
    private Long recipeId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID (0为根评论)
     */
    private Long parentId;

    /**
     * 根评论ID (用于快速查询某楼层所有回复)
     */
    private Long rootId;

    /**
     * 点赞数 (冗余字段，便于展示)
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;
}
