package com.zyyyys.culinarywhispers.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食谱步骤实体类
 * @author zyyyys
 */
@Data
@TableName("t_rcp_step")
public class RecipeStep implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 食谱ID
     */
    private Long recipeId;

    /**
     * 步骤序号
     */
    private Integer stepNo;

    /**
     * 步骤描述
     */
    @TableField("`desc`")
    private String desc;

    /**
     * 步骤图
     */
    private String imgUrl;

    /**
     * 步骤视频片段
     */
    private String videoUrl;

    /**
     * 该步骤耗时
     */
    private Integer timeCost;

    /**
     * 语音讲解
     */
    private String voiceUrl;

    /**
     * 是否关键步骤
     */
    private Boolean isKeyStep;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
}
