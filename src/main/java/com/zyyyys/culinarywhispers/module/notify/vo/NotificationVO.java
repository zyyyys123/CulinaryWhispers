package com.zyyyys.culinarywhispers.module.notify.vo;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@Schema(name = "NotificationVO", description = "站内通知")
public class NotificationVO {
    @Schema(description = "通知ID", example = "1001")
    private Long id;
    @Schema(description = "通知类型：1-评论回复,2-评论食谱,3-点赞食谱,4-收藏食谱,5-点赞评论", example = "2")
    private Integer type;
    @Schema(description = "目标类型：1-食谱,2-评论", example = "1")
    private Integer targetType;
    @Schema(description = "目标ID（食谱ID/评论ID）", example = "20001")
    private Long targetId;
    @Schema(description = "通知内容", example = "有人评论了你的食谱：太好吃了！")
    private String content;
    @Schema(description = "是否已读：0-未读,1-已读", example = "0")
    private Integer isRead;
    @Schema(description = "创建时间", example = "2026-03-07T12:30:00")
    private LocalDateTime createTime;

    @Schema(description = "触发者用户ID", example = "12")
    private Long fromUserId;
    @Schema(description = "触发者昵称", example = "小厨神")
    private String fromNickname;
    @Schema(description = "触发者头像URL", example = "/api/uploads/avatar.png")
    private String fromAvatarUrl;
}
