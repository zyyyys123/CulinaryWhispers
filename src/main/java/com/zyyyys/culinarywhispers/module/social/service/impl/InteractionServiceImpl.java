package com.zyyyys.culinarywhispers.module.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.module.social.entity.Interaction;
import com.zyyyys.culinarywhispers.module.social.event.InteractionEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.InteractionMapper;
import com.zyyyys.culinarywhispers.module.social.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 互动服务实现类
 * @author zyyyys
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl extends ServiceImpl<InteractionMapper, Interaction> implements InteractionService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 切换互动状态（点赞/取消点赞、收藏/取消收藏）
     * @param userId 用户ID
     * @param targetType 目标类型 (1: 配方, 2: 评论)
     * @param targetId 目标ID (配方ID或评论ID)
     * @param actionType 操作类型 (1: 点赞, 2: 取消点赞, 3: 收藏, 4: 取消收藏)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleInteraction(Long userId, Integer targetType, Long targetId, Integer actionType) {
        // 1. 参数校验
        if (userId == null || targetType == null || targetId == null || actionType == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        // 2. 检查是否已存在互动记录 (幂等性检查)
        // 使用 selectOne 而不是 exists，虽然性能稍差一点点，但在当前规模下可接受且通用
        LambdaQueryWrapper<Interaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Interaction::getUserId, userId)
               .eq(Interaction::getTargetType, targetType)
               .eq(Interaction::getTargetId, targetId)
               .eq(Interaction::getActionType, actionType);
        
        Interaction existing = this.getOne(wrapper);
        boolean isAdd;

        if (existing != null) {
            // 3. 已存在 -> 取消互动 (删除记录)
            this.removeById(existing.getId());
            isAdd = false;
            log.info("Interaction removed. User: {}, Target: {}-{}, Action: {}", userId, targetType, targetId, actionType);
        } else {
            // 4. 不存在 -> 添加互动 (插入记录)
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setTargetType(targetType);
            interaction.setTargetId(targetId);
            interaction.setActionType(actionType);
            interaction.setGmtCreate(LocalDateTime.now());
            this.save(interaction);
            isAdd = true;
            log.info("Interaction added. User: {}, Target: {}-{}, Action: {}", userId, targetType, targetId, actionType);
        }

        // 5. 发布事件 (异步/同步通知统计更新)
        eventPublisher.publishEvent(new InteractionEvent(this, userId, targetType, targetId, actionType, isAdd));
    }
}
