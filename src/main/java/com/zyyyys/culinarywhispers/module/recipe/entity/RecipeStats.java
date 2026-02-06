package com.zyyyys.culinarywhispers.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食谱统计实体类
 * @author zyyyys
 */
@Data
@TableName("t_rcp_stats")
public class RecipeStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "recipe_id", type = IdType.INPUT)
    private Long recipeId;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点赞量
     */
    private Long likeCount;

    /**
     * 收藏量
     */
    private Long collectCount;

    /**
     * 评论量
     */
    private Long commentCount;

    /**
     * 分享量
     */
    private Long shareCount;

    /**
     * 跟做人数
     */
    private Integer tryCount;

    /**
     * 综合评分
     */
    private BigDecimal score;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
