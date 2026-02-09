package com.zyyyys.culinarywhispers.module.social.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;

/**
 * 评论服务接口
 * @author zyyyys
 */
public interface CommentService extends IService<Comment> {

    /**
     * 发布评论
     * @param userId 用户ID
     * @param recipeId 食谱ID
     * @param content 内容
     * @param parentId 父评论ID
     * @return 评论ID
     */
    Long addComment(Long userId, Long recipeId, String content, Long parentId);

    /**
     * 删除评论
     * @param userId 操作者ID (用于鉴权)
     * @param commentId 评论ID
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 获取食谱的"跟做"作业 (带图片的评论)
     * @param recipeId 食谱ID
     * @param limit 限制数量
     * @return 评论列表
     */
    java.util.List<Comment> getRecipeWorks(Long recipeId, int limit);

    /**
     * 获取食谱评论列表
     * @param recipeId 食谱ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页列表
     */
    Page<Comment> listComments(Long recipeId, int page, int size);
}
