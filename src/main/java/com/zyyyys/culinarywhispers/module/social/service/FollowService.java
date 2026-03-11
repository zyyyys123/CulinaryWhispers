package com.zyyyys.culinarywhispers.module.social.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import com.zyyyys.culinarywhispers.module.social.vo.FollowVO;

/**
 * 关注服务接口
 * @author zyyyys
 */
public interface FollowService extends IService<Follow> {

    /**
     * 关注用户
     * @param followerId 粉丝ID (当前用户)
     * @param followingId 被关注者ID
     */
    void followUser(Long followerId, Long followingId);

    /**
     * 取消关注
     * @param followerId 粉丝ID
     * @param followingId 被关注者ID
     */
    void unfollowUser(Long followerId, Long followingId);

    /**
     * 获取粉丝列表
     */
    Page<FollowVO> listFollowers(Long userId, int page, int size);

    /**
     * 获取关注列表
     */
    Page<FollowVO> listFollowing(Long userId, int page, int size);

    void updateRemark(Long followerId, Long followingId, String remarkName);

    /**
     * 检查是否已关注
     */
    boolean isFollowing(Long followerId, Long followingId);
}
