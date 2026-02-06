package com.zyyyys.culinarywhispers.module.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食谱分类 Mapper 接口
 * @author zyyyys
 */
@Mapper
public interface RecipeCategoryMapper extends BaseMapper<RecipeCategory> {
}
