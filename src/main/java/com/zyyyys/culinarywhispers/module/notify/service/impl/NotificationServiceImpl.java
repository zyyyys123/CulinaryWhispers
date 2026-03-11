package com.zyyyys.culinarywhispers.module.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.notify.entity.Notification;
import com.zyyyys.culinarywhispers.module.notify.mapper.NotificationMapper;
import com.zyyyys.culinarywhispers.module.notify.service.NotificationService;
import com.zyyyys.culinarywhispers.module.notify.vo.NotificationVO;
import com.zyyyys.culinarywhispers.module.user.entity.User;
import com.zyyyys.culinarywhispers.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final UserService userService;

    @Override
    public Page<NotificationVO> pageMyNotifications(Long userId, int page, int size, Integer type) {
        Page<Notification> p = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getToUserId, userId)
                .orderByDesc(Notification::getGmtCreate);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        this.page(p, wrapper);

        List<Notification> records = p.getRecords();
        Set<Long> fromUserIds = records.stream()
                .map(Notification::getFromUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = fromUserIds.isEmpty()
                ? Map.of()
                : userService.listByIds(fromUserIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<NotificationVO> voList = records.stream().map(n -> {
            NotificationVO vo = new NotificationVO();
            vo.setId(n.getId());
            vo.setType(n.getType());
            vo.setTargetType(n.getTargetType());
            vo.setTargetId(n.getTargetId());
            vo.setContent(n.getContent());
            vo.setIsRead(n.getIsRead());
            vo.setCreateTime(n.getGmtCreate());
            vo.setFromUserId(n.getFromUserId());
            User u = userMap.get(n.getFromUserId());
            if (u != null) {
                vo.setFromNickname(u.getNickname());
                vo.setFromAvatarUrl(u.getAvatarUrl());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<NotificationVO> result = new Page<>(page, size);
        result.setTotal(p.getTotal());
        result.setSize(p.getSize());
        result.setCurrent(p.getCurrent());
        result.setPages(p.getPages());
        result.setRecords(voList);
        return result;
    }

    @Override
    public long countUnread(Long userId, Integer type) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getToUserId, userId)
                .eq(Notification::getIsRead, 0);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        return this.count(wrapper);
    }

    @Override
    public Map<Integer, Long> countUnreadByType(Long userId) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.select("type", "COUNT(1) AS cnt")
                .eq("to_user_id", userId)
                .eq("is_read", 0)
                .groupBy("type");
        List<Map<String, Object>> rows = this.listMaps(wrapper);
        Map<Integer, Long> result = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Object t = row.get("type");
            Object c = row.get("cnt");
            if (t == null || c == null) {
                continue;
            }
            Integer type = Integer.valueOf(String.valueOf(t));
            Long cnt = Long.valueOf(String.valueOf(c));
            result.put(type, cnt);
        }
        return result;
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        Notification n = this.getById(notificationId);
        if (n == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!Objects.equals(n.getToUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        n.setIsRead(1);
        this.updateById(n);
    }

    @Override
    public void markAllRead(Long userId, Integer type) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getToUserId, userId)
                .eq(Notification::getIsRead, 0);
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.set(Notification::getIsRead, 1);
        this.update(wrapper);
    }
}
