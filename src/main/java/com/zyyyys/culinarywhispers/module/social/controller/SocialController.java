package com.zyyyys.culinarywhispers.module.social.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.recipe.vo.RecipePageVO;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import com.zyyyys.culinarywhispers.module.social.service.CommentService;
import com.zyyyys.culinarywhispers.module.social.service.FollowService;
import com.zyyyys.culinarywhispers.module.social.service.InteractionService;
import com.zyyyys.culinarywhispers.module.social.vo.CommentVO;
import com.zyyyys.culinarywhispers.module.social.vo.FollowVO;
import com.zyyyys.culinarywhispers.module.social.vo.InteractionStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 社交互动控制器
 * @author zyyyys
 */
@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final InteractionService interactionService;
    private final CommentService commentService;
    private final FollowService followService;

    /**
     * 互动操作 (点赞/取消点赞, 收藏/取消收藏)
     * @param targetType 目标类型 (1-食谱, 2-评论)
     * @param targetId 目标ID
     * @param actionType 动作类型 (1-点赞, 2-收藏, 3-分享)
     */
    @PostMapping("/interact")
    public Result<Void> interact(@RequestParam Integer targetType,
                                 @RequestParam Long targetId,
                                 @RequestParam Integer actionType) {
        Long userId = SecurityUtil.getUserId();
        interactionService.toggleInteraction(userId, targetType, targetId, actionType);
        return Result.success();
    }

    @GetMapping("/interact/status")
    public Result<InteractionStatusVO> getInteractionStatus(@RequestParam Integer targetType,
                                                            @RequestParam Long targetId) {
        Long userId = SecurityUtil.getOptionalUserId();
        return Result.success(interactionService.getStatus(userId, targetType, targetId));
    }

    // ================== 评论接口 ==================

    /**
     * 发布评论
     * @param recipeId 食谱ID
     * @param content 内容
     * @param parentId 父评论ID (可选)
     */
    @PostMapping("/comment")
    public Result<Long> addComment(@RequestParam Long recipeId,
                                   @RequestParam String content,
                                   @RequestParam(required = false) Long parentId) {
        Long userId = SecurityUtil.getUserId();
        Long commentId = commentService.addComment(userId, recipeId, content, parentId);
        return Result.success(commentId);
    }

    /**
     * 删除评论
     * @param commentId 评论ID
     */
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = SecurityUtil.getUserId();
        commentService.deleteComment(userId, commentId);
        return Result.success();
    }

    /**
     * 获取食谱评论列表
     * @param recipeId 食谱ID
     * @param page 页码
     * @param size 每页大小
     */
    @GetMapping("/comment/list")
    public Result<Page<CommentVO>> listComments(@RequestParam Long recipeId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.listComments(recipeId, page, size));
    }

    @GetMapping("/comment/{commentId}")
    public Result<CommentVO> getComment(@PathVariable Long commentId) {
        return Result.success(commentService.getComment(commentId));
    }

    // ================== 关注接口 ==================

    /**
     * 关注用户
     * @param followingId 被关注者ID
     */
    @PostMapping("/follow/{followingId}")
    public Result<Void> followUser(@PathVariable Long followingId) {
        Long userId = SecurityUtil.getUserId();
        followService.followUser(userId, followingId);
        return Result.success();
    }

    /**
     * 取消关注
     * @param followingId 被关注者ID
     */
    @PostMapping("/unfollow/{followingId}")
    public Result<Void> unfollowUser(@PathVariable Long followingId) {
        Long userId = SecurityUtil.getUserId();
        followService.unfollowUser(userId, followingId);
        return Result.success();
    }

    /**
     * 获取我的粉丝列表
     */
    @GetMapping("/followers")
    public Result<Page<FollowVO>> listMyFollowers(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(followService.listFollowers(userId, page, size));
    }

    /**
     * 获取我的关注列表
     */
    @GetMapping("/following")
    public Result<Page<FollowVO>> listMyFollowing(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(followService.listFollowing(userId, page, size));
    }

    @PostMapping("/follow/remark/{followingId}")
    public Result<Void> updateRemark(@PathVariable Long followingId, @RequestParam(required = false) String remarkName) {
        Long userId = SecurityUtil.getUserId();
        followService.updateRemark(userId, followingId, remarkName);
        return Result.success();
    }

    /**
     * 获取我的收藏食谱列表
     */
    @GetMapping("/collect/recipes")
    public Result<Page<RecipePageVO>> listMyCollectedRecipes(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(interactionService.pageCollectedRecipes(userId, page, size));
    }
}
