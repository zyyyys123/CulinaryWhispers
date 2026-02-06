package com.zyyyys.culinarywhispers.module.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食谱主表 Mapper 接口
 * @author zyyyys
 */
@Mapper
public interface RecipeInfoMapper extends BaseMapper<RecipeInfo> {
}
