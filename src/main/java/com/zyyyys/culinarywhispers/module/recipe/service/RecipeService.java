package com.zyyyys.culinarywhispers.module.recipe.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipePublishDTO;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipeQueryDTO;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipeDetailVO;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;

/**
 * 食谱服务接口
 * @author zyyyys
 */
public interface RecipeService extends IService<RecipeInfo> {

    /**
     * 发布食谱
     * @param userId 作者ID
     * @param publishDTO 发布信息
     * @return 食谱ID
     */
    Long publish(Long userId, RecipePublishDTO publishDTO);

    /**
     * 获取食谱详情
     * @param id 食谱ID
     * @return 详情VO
     */
    RecipeDetailVO getDetail(Long id);

    /**
     * 分页查询
     * @param queryDTO 查询条件
     * @return 分页列表
     */
    Page<RecipePageVO> pageList(RecipeQueryDTO queryDTO);

    Page<RecipePageVO> pageListPersonalized(Long userId, RecipeQueryDTO queryDTO);

    /**
     * 热门食谱列表（带缓存）
     * @param queryDTO page/size
     * @return 分页列表
     */
    Page<RecipePageVO> pageHot(RecipeQueryDTO queryDTO);

    /**
     * 更新食谱
     * @param userId 用户ID
     * @param recipeId 食谱ID
     * @param publishDTO 更新信息
     */
    void updateRecipe(Long userId, Long recipeId, RecipePublishDTO publishDTO);

    /**
     * 删除食谱
     * @param userId 用户ID
     * @param recipeId 食谱ID
     */
    void deleteRecipe(Long userId, Long recipeId);
}
