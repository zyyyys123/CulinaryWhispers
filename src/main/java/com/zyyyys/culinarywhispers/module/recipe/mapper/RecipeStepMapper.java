package com.zyyyys.culinarywhispers.module.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食谱步骤 Mapper 接口
 * @author zyyyys
 */
@Mapper
public interface RecipeStepMapper extends BaseMapper<RecipeStep> {
}
