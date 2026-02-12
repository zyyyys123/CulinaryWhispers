package com.zyyyys.culinarywhispers.module.recipe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipePublishDTO;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipeQueryDTO;
import com.zyyyys.culinarywhispers.module.recipe.service.RecipeService;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipeDetailVO;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;

/**
 * 食谱控制器
 * @author zyyyys
 */
@RestController
@RequestMapping("/api/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * 发布食谱
     */
    @PostMapping("/publish")
    public Result<Long> publish(@RequestBody RecipePublishDTO publishDTO) {
        Long userId = SecurityUtil.getUserId();
        Long recipeId = recipeService.publish(userId, publishDTO);
        return Result.success(recipeId);
    }

    /**
     * 获取食谱详情
     */
    @GetMapping("/{id}")
    public Result<RecipeDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(recipeService.getDetail(id));
    }

    /**
     * 分页查询食谱列表
     */
    @GetMapping("/list")
    public Result<Page<RecipePageVO>> list(RecipeQueryDTO queryDTO) {
        return Result.success(recipeService.pageList(queryDTO));
    }

    @GetMapping("/recommend")
    public Result<Page<RecipePageVO>> recommend(RecipeQueryDTO queryDTO) {
        Long userId = SecurityUtil.getOptionalUserId();
        if (userId == null) {
            return Result.success(recipeService.pageList(queryDTO));
        }
        return Result.success(recipeService.pageListPersonalized(userId, queryDTO));
    }
}
