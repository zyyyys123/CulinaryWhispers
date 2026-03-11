package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.social.entity.Follow;
import com.zyyyys.culinarywhispers.module.social.mapper.FollowMapper;
import com.zyyyys.culinarywhispers.module.social.service.FollowService;
import com.zyyyys.culinarywhispers.module.social.vo.FollowVO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.entity.UserProfile;
import com.zyyyys.culinarywhispers.module.user.mapper.UserMapper;
import com.zyyyys.culinarywhispers.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 关注服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

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
    public Page<FollowVO> listFollowers(Long userId, int page, int size) {
        Page<Follow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowingId, userId)
               .eq(Follow::getStatus, 1)
               .orderByDesc(Follow::getGmtCreate);
        this.page(pageParam, wrapper);

        List<Follow> records = pageParam.getRecords();
        if (records == null || records.isEmpty()) {
            Page<FollowVO> result = new Page<>(page, size);
            result.setTotal(pageParam.getTotal());
            result.setSize(pageParam.getSize());
            result.setCurrent(pageParam.getCurrent());
            result.setPages(pageParam.getPages());
            result.setRecords(List.of());
            return result;
        }
        Set<Long> followerIds = records.stream().map(Follow::getFollowerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FollowVO.UserVO> userMap = buildUserMap(followerIds);

        Set<Long> mutualSet = followerIds.isEmpty()
                ? Collections.emptySet()
                : this.list(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId)
                        .in(Follow::getFollowingId, followerIds)
                        .eq(Follow::getStatus, 1)).stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toSet());

        List<FollowVO> voList = records.stream().map(f -> {
            FollowVO vo = new FollowVO();
            vo.setUserId(f.getFollowerId());
            vo.setUser(userMap.get(f.getFollowerId()));
            vo.setGmtCreate(f.getGmtCreate());
            vo.setIsMutual(mutualSet.contains(f.getFollowerId()));
            return vo;
        }).collect(Collectors.toList());

        Page<FollowVO> result = new Page<>(page, size);
        result.setTotal(pageParam.getTotal());
        result.setSize(pageParam.getSize());
        result.setCurrent(pageParam.getCurrent());
        result.setPages(pageParam.getPages());
        result.setRecords(voList);
        return result;
    }

    /**
     * 查询关注的用户列表
     * @param userId 关注者ID
     * @param page 页码
     * @param size 每页数量
     * @return 关注的用户分页列表
     */
    @Override
    public Page<FollowVO> listFollowing(Long userId, int page, int size) {
        Page<Follow> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId)
               .eq(Follow::getStatus, 1)
               .orderByDesc(Follow::getGmtCreate);
        this.page(pageParam, wrapper);

        List<Follow> records = pageParam.getRecords();
        if (records == null || records.isEmpty()) {
            Page<FollowVO> result = new Page<>(page, size);
            result.setTotal(pageParam.getTotal());
            result.setSize(pageParam.getSize());
            result.setCurrent(pageParam.getCurrent());
            result.setPages(pageParam.getPages());
            result.setRecords(List.of());
            return result;
        }
        Set<Long> followingIds = records.stream().map(Follow::getFollowingId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FollowVO.UserVO> userMap = buildUserMap(followingIds);

        Set<Long> mutualSet = followingIds.isEmpty()
                ? Collections.emptySet()
                : this.list(new LambdaQueryWrapper<Follow>()
                        .in(Follow::getFollowerId, followingIds)
                        .eq(Follow::getFollowingId, userId)
                        .eq(Follow::getStatus, 1)).stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toSet());

        List<FollowVO> voList = records.stream().map(f -> {
            FollowVO vo = new FollowVO();
            vo.setUserId(f.getFollowingId());
            vo.setUser(userMap.get(f.getFollowingId()));
            vo.setGmtCreate(f.getGmtCreate());
            vo.setIsMutual(mutualSet.contains(f.getFollowingId()));
            vo.setRemarkName(f.getRemarkName());
            return vo;
        }).collect(Collectors.toList());

        Page<FollowVO> result = new Page<>(page, size);
        result.setTotal(pageParam.getTotal());
        result.setSize(pageParam.getSize());
        result.setCurrent(pageParam.getCurrent());
        result.setPages(pageParam.getPages());
        result.setRecords(voList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(Long followerId, Long followingId, String remarkName) {
        Follow existing = getFollowRecord(followerId, followingId);
        if (existing == null || existing.getStatus() == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        String v = remarkName == null ? null : remarkName.trim();
        if (v != null && v.length() > 64) {
            throw new BusinessException(400, "备注名过长");
        }
        existing.setRemarkName(v);
        existing.setGmtModified(LocalDateTime.now());
        this.updateById(existing);
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

    private Map<Long, FollowVO.UserVO> buildUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users == null ? Map.of() : users.stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<UserProfile> profiles = userProfileMapper.selectBatchIds(userIds);
        Map<Long, UserProfile> profileMap = profiles == null ? Map.of() : profiles.stream().collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (a, b) -> a));

        return userIds.stream().collect(Collectors.toMap(Function.identity(), id -> {
            User u = userMap.get(id);
            UserProfile p = profileMap.get(id);
            FollowVO.UserVO vo = new FollowVO.UserVO();
            if (u != null) {
                vo.setId(u.getId());
                vo.setUsername(u.getUsername());
                vo.setNickname(u.getNickname());
                vo.setAvatarUrl(u.getAvatarUrl());
            }
            if (p != null) {
                vo.setIsMasterChef(p.getIsMasterChef());
                vo.setMasterTitle(p.getMasterTitle());
                vo.setBgImageUrl(p.getBgImageUrl());
            }
            return vo;
        }));
    }
}
