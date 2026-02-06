package com.zyyyys.culinarywhispers.module.recipe.vo;

import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStep;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 食谱详情视图对象
 * @author zyyyys
 */
@Data
public class RecipeDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 食谱基本信息
     */
    private RecipeInfo info;

    /**
     * 食谱统计信息
     */
    private RecipeStats stats;

    /**
     * 食谱步骤列表
     */
    private List<RecipeStep> steps;

    /**
     * 作者信息
     */
    private AuthorVO author;

    @Data
    public static class AuthorVO implements Serializable {
        private Long id;
        private String nickname;
        private String avatarUrl;
    }
}
