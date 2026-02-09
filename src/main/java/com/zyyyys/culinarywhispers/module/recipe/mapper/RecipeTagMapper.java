package com.zyyyys.culinarywhispers.module.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 食谱标签 Mapper 接口
 * @author zyyyys
 */
@Mapper
public interface RecipeTagMapper extends BaseMapper<RecipeTag> {

    /**
     * 根据食谱ID查询标签名称列表
     * @param recipeId 食谱ID
     * @return 标签名称列表
     */
    List<String> selectTagNamesByRecipeId(@Param("recipeId") Long recipeId);
}
