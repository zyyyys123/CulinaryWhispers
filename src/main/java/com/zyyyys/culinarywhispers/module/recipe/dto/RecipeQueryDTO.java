package com.zyyyys.culinarywhispers.module.recipe.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 食谱查询参数
 * @author zyyyys
 */
@Data
public class RecipeQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer page = 1;
    private Integer size = 10;

    /**
     * 搜索关键字 (标题/描述)
     */
    private String keyword;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 排序字段: gmt_create, score, view_count, like_count
     */
    private String sortBy;
    
    /**
     * 作者ID
     */
    private Long authorId;
}
