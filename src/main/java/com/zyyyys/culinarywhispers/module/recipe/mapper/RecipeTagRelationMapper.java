package com.zyyyys.culinarywhispers.module.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeTagRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 食谱标签关联 Mapper 接口
 * @author zyyyys
 */
@Mapper
public interface RecipeTagRelationMapper extends BaseMapper<RecipeTagRelation> {
}
