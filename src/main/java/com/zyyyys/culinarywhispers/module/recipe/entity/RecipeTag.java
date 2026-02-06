package com.zyyyys.culinarywhispers.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 食谱标签实体
 */
@Data
@TableName("t_rcp_tag")
public class RecipeTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer type; // 1-General, 2-Ingredient, 3-Scene
    private Integer useCount;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}
