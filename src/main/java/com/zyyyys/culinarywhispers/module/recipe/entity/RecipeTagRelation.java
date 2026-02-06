package com.zyyyys.culinarywhispers.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 食谱 - 标签关联实体
 */
@Data
@TableName("t_rcp_tag_relation")
public class RecipeTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipeId;
    private Long tagId;
}
