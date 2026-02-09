package com.zyyyys.culinarywhispers.module.search.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.search.entity.RecipeDocument;
import com.zyyyys.culinarywhispers.module.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;

/**
 * 搜索控制器
 * @author zyyyys
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索食谱
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    @GetMapping("/recipe")
    public Result<Page<RecipeDocument>> searchRecipe(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return Result.success(searchService.searchRecipe(keyword, page, size));
    }

    /**
     * 个性化搜索 (需登录)
     * @param keyword 关键词 (可选)
     */
    @GetMapping("/personalized")
    public Result<Page<RecipeDocument>> searchPersonalized(@RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getUserId(); // 可能抛出异常如果未登录，或者返回null
        // 假设 SecurityUtil.getUserId() 在未登录时抛出异常或返回null。
        // 如果未登录，降级为普通搜索
        if (userId == null) {
            return Result.success(searchService.searchRecipe(keyword != null ? keyword : "", page, size));
        }
        return Result.success(searchService.searchPersonalized(userId, keyword, page, size));
    }
}
