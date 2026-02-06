package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import com.zyyyys.culinarywhispers.module.social.mapper.FollowMapper;
import com.zyyyys.culinarywhispers.module.social.service.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 关注服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    /**
     * 关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     */ 
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(400, "不能关注自己");
        }

        // 幂等性检查：是否已关注
        Follow existing = getFollowRecord(followerId, followingId);
        if (existing != null) {
            // 如果已存在记录但状态为取消(0)，则重新激活
            if (existing.getStatus() == 0) {
                existing.setStatus(1);
                existing.setGmtModified(LocalDateTime.now());
                this.updateById(existing);
            }
            // 如果已经是关注状态(1)，则无需操作
            return;
        }

        // 创建新关注记录
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setStatus(1);
        follow.setGmtCreate(LocalDateTime.now());
        follow.setGmtModified(LocalDateTime.now());
        this.save(follow);
        
        log.info("User {} followed user {}", followerId, followingId);
    }

    /**
     * 取消关注用户
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     */ 
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long followerId, Long followingId) {
        Follow existing = getFollowRecord(followerId, followingId);
        if (existing == null || existing.getStatus() == 0) {
            return; // 未关注，直接返回
        }

        existing.setStatus(0); // 软删除/状态变更
        existing.setGmtModified(LocalDateTime.now());
        this.updateById(existing);
        
        log.info("User {} unfollowed user {}", followerId, followingId);
    }

    /**
     * 查询关注者列表
     * @param userId 被关注者ID
     * @param page 页码
     * @param size 每页数量
     * @return 关注者分页列表
     */
    @Override
    public Page<Follow> listFollowers(Long userId, int page, int size) {
        Page<Follow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, userId)
               .eq(Follow::getStatus, 1)
               .orderByDesc(Follow::getGmtCreate);
        return this.page(pageParam, wrapper);
    }

    /**
     * 查询关注的用户列表
     * @param userId 关注者ID
     * @param page 页码
     * @param size 每页数量
     * @return 关注的用户分页列表
     */
    @Override
    public Page<Follow> listFollowing(Long userId, int page, int size) {
        Page<Follow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId)
               .eq(Follow::getStatus, 1)
               .orderByDesc(Follow::getGmtCreate);
        return this.page(pageParam, wrapper);
    }

    /**
     * 检查是否关注
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否关注
     */
    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        Follow record = getFollowRecord(followerId, followingId);
        return record != null && record.getStatus() == 1;
    }

    private Follow getFollowRecord(Long followerId, Long followingId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId)
               .eq(Follow::getFollowingId, followingId);
        return this.getOne(wrapper);
    }
}
