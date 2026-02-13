package com.zyyyys.culinarywhispers.module.notify.listener;

import com.zyyyys.culinarywhispers.module.notify.entity.Notification;
import com.zyyyys.culinarywhispers.module.notify.service.NotificationService;
import com.zyyyys.culinarywhispers.module.recipe.entity.RecipeInfo;
import com.zyyyys.culinarywhispers.module.recipe.mapper.RecipeInfoMapper;
import com.zyyyys.culinarywhispers.module.social.entity.Comment;
import com.zyyyys.culinarywhispers.module.social.event.InteractionEvent;
import com.zyyyys.culinarywhispers.module.social.mapper.CommentMapper;
import com.zyyyys.culinarywhispers.module.user.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final RecipeInfoMapper recipeInfoMapper;
    private final CommentMapper commentMapper;
    private final UserPointsService pointsService;

    @EventListener
    public void handleInteraction(InteractionEvent event) {
        if (!event.isAdd()) {
            return;
        }
        Long fromUserId = event.getUserId();
        if (fromUserId == null) {
            return;
        }

        if (event.getTargetType() == 1 && (event.getActionType() == 1 || event.getActionType() == 2)) {
            RecipeInfo info = recipeInfoMapper.selectById(event.getTargetId());
            if (info == null || info.getAuthorId() == null) {
                return;
            }
            Long toUserId = info.getAuthorId();
            if (Objects.equals(fromUserId, toUserId)) {
                return;
            }
            Notification n = new Notification();
            n.setFromUserId(fromUserId);
            n.setToUserId(toUserId);
            n.setType(event.getActionType() == 1 ? 3 : 4);
            n.setTargetType(1);
            n.setTargetId(event.getTargetId());
            n.setContent(event.getActionType() == 1 ? "点赞了你的食谱" : "收藏了你的食谱");
            n.setIsRead(0);
            n.setGmtCreate(LocalDateTime.now());
            notificationService.save(n);
            if (event.getActionType() == 1) {
                pointsService.addPoints(toUserId, 2, 3, "食谱被点赞");
            } else {
                pointsService.addPoints(toUserId, 5, 6, "食谱被收藏");
            }
            return;
        }

        if (event.getTargetType() == 2 && event.getActionType() == 1) {
            Comment c = commentMapper.selectById(event.getTargetId());
            if (c == null || c.getUserId() == null) {
                return;
            }
            Long toUserId = c.getUserId();
            if (Objects.equals(fromUserId, toUserId)) {
                return;
            }
            Notification n = new Notification();
            n.setFromUserId(fromUserId);
            n.setToUserId(toUserId);
            n.setType(5);
            n.setTargetType(2);
            n.setTargetId(event.getTargetId());
            n.setContent("点赞了你的评论");
            n.setIsRead(0);
            n.setGmtCreate(LocalDateTime.now());
            notificationService.save(n);
            pointsService.addPoints(toUserId, 1, 3, "评论被点赞");
        }
    }
}
