package com.zyyyys.culinarywhispers.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食谱分类实体类
 * @author zyyyys
 */
@Data
@TableName("t_rcp_category")
public class RecipeCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID
     */
    private Integer parentId;

    /**
     * 层级: 1-一级, 2-二级, 3-三级
     */
    private Integer level;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 图标URL
     */
    private String iconUrl;

    /**
     * 是否可见
     */
    private Boolean isVisible;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime gmtModified;
}
