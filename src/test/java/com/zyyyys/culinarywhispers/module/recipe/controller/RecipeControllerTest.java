package com.zyyyys.culinarywhispers.module.recipe.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipePublishDTO;
import com.zyyyys.culinarywhispers.module.recipe.dto.RecipeQueryDTO;
import com.zyyyys.culinarywhispers.module.recipe.service.RecipeService;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipeDetailVO;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecipeControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void publish_getDetail_list() {
        RecipeService recipeService = mock(RecipeService.class);
        RecipeController controller = new RecipeController(recipeService);

        UserContext.setUserId(1L);
        when(recipeService.publish(eq(1L), any(RecipePublishDTO.class))).thenReturn(100L);
        when(recipeService.getDetail(100L)).thenReturn(new RecipeDetailVO());
        when(recipeService.pageList(any(RecipeQueryDTO.class))).thenReturn(new Page<RecipePageVO>());

        Result<Long> publish = controller.publish(new RecipePublishDTO());
        assertEquals(0, publish.getCode());

        Result<RecipeDetailVO> detail = controller.getDetail(100L);
        assertEquals(0, detail.getCode());

        Result<Page<RecipePageVO>> list = controller.list(new RecipeQueryDTO());
        assertEquals(0, list.getCode());
    }
}

