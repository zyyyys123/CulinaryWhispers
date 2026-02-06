package com.zyyyys.culinarywhispers.module.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食谱统计 Mapper 接口
 * @author zyyyys
 */
@Mapper
public interface RecipeStatsMapper extends BaseMapper<RecipeStats> {
}
