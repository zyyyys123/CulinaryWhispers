package com.zyyyys.culinarywhispers.module.social.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import com.zyyyys.culinarywhispers.module.social.service.CommentService;
import com.zyyyys.culinarywhispers.module.social.service.FollowService;
import com.zyyyys.culinarywhispers.module.social.service.InteractionService;
import com.zyyyys.culinarywhispers.module.social.vo.CommentVO;
import com.zyyyys.culinarywhispers.module.social.vo.InteractionStatusVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SocialControllerTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void interact_comment_follow_endpoints() {
        InteractionService interactionService = mock(InteractionService.class);
        CommentService commentService = mock(CommentService.class);
        FollowService followService = mock(FollowService.class);
        SocialController controller = new SocialController(interactionService, commentService, followService);

        UserContext.setUserId(1L);

        Result<Void> interact = controller.interact(1, 100L, 1);
        assertEquals(0, interact.getCode());
        verify(interactionService).toggleInteraction(1L, 1, 100L, 1);

        when(commentService.addComment(eq(1L), eq(100L), anyString(), any())).thenReturn(10L);
        Result<Long> commentId = controller.addComment(100L, "hi", null);
        assertEquals(0, commentId.getCode());

        when(commentService.listComments(eq(100L), anyInt(), anyInt())).thenReturn(new Page<CommentVO>());
        Result<Page<CommentVO>> comments = controller.listComments(100L, 1, 10);
        assertEquals(0, comments.getCode());

        Result<Void> follow = controller.followUser(2L);
        assertEquals(0, follow.getCode());
        verify(followService).followUser(1L, 2L);

        when(followService.listFollowing(eq(1L), anyInt(), anyInt())).thenReturn(new Page<Follow>());
        Result<Page<Follow>> following = controller.listMyFollowing(1, 10);
        assertEquals(0, following.getCode());
    }

    @Test
    void getInteractionStatus_notLogin_returnsDefault() {
        InteractionService interactionService = mock(InteractionService.class);
        CommentService commentService = mock(CommentService.class);
        FollowService followService = mock(FollowService.class);
        SocialController controller = new SocialController(interactionService, commentService, followService);

        InteractionStatusVO status = new InteractionStatusVO();
        status.setLiked(false);
        status.setCollected(false);
        when(interactionService.getStatus(isNull(), eq(1), eq(100L))).thenReturn(status);

        Result<InteractionStatusVO> res = controller.getInteractionStatus(1, 100L);
        assertEquals(0, res.getCode());
        assertNotNull(res.getData());
        assertFalse(res.getData().getLiked());
        assertFalse(res.getData().getCollected());
    }
}
