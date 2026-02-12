package com.zyyyys.culinarywhispers.module.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.user.entity.PointsRecord;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class UserPointsControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void signIn_and_history() {
        UserPointsService pointsService = mock(UserPointsService.class);
        UserPointsController controller = new UserPointsController(pointsService);

        UserContext.setUserId(1L);
        when(pointsService.signIn(1L)).thenReturn(10);
        when(pointsService.page(any(Page.class), any())).thenReturn(new Page<PointsRecord>());

        Result<Integer> signIn = controller.signIn();
        assertEquals(0, signIn.getCode());

        Result<Page<PointsRecord>> history = controller.getPointsHistory(1, 10);
        assertEquals(0, history.getCode());
        verify(pointsService).page(any(Page.class), any());
    }
}

