package com.zyyyys.culinarywhispers.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食谱主表实体类
 * @author zyyyys
 */
@Data
@TableName("t_rcp_info")
public class RecipeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 标题
     */
    private String title;

    /**
     * 封面图
     */
    private String coverUrl;

    /**
     * 视频地址
     */
    private String videoUrl;

    /**
     * 简介
     */
    private String description;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 难度: 1-5
     */
    private Integer difficulty;

    /**
     * 耗时(分钟)
     */
    private Integer timeCost;

    /**
     * 卡路里(千卡)
     */
    private Integer calories;

    /**
     * 蛋白质(g)
     */
    private BigDecimal protein;

    /**
     * 脂肪(g)
     */
    private BigDecimal fat;

    /**
     * 碳水(g)
     */
    private BigDecimal carbs;

    /**
     * 综合评分
     */
    private BigDecimal score;

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
     * 状态: 0-草稿, 1-审核中, 2-发布, 3-驳回, 4-下架
     */
    private Integer status;

    /**
     * 审核意见
     */
    private String auditMsg;

    /**
     * 标签JSON数组 (冗余字段，便于列表查询)
     */
    private String tags;

    /**
     * 小贴士
     */
    private String tips;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;

    @TableLogic
    private Integer isDeleted;

    @Version
    private Integer version;
}
