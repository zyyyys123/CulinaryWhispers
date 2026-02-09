package com.zyyyys.culinarywhispers.module.search.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;

/**
 * 搜索服务接口
 * @author zyyyys
 */
public interface SearchService {

    /**
     * 同步食谱数据到ES
     * @param recipeId 食谱ID
     */
    void syncRecipe(Long recipeId);

    /**
     * 从ES删除食谱数据
     * @param recipeId 食谱ID
     */
    void deleteRecipe(Long recipeId);

    /**
     * 搜索食谱
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<RecipeDocument> searchRecipe(String keyword, int page, int size);

    /**
     * 个性化搜索食谱 (基于用户画像过滤忌口、加权喜好)
     * @param userId 用户ID
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    Page<RecipeDocument> searchPersonalized(Long userId, String keyword, int page, int size);
}
