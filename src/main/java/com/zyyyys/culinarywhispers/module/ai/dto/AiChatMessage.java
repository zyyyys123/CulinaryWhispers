package com.zyyyys.culinarywhispers.module.ai.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(name = "AiChatMessage", description = "AI 历史消息")
public class AiChatMessage {
    @Schema(description = "角色", example = "user", allowableValues = {"user", "assistant"})
    private String role;
    @Schema(description = "消息内容", example = "我想做一道低脂的晚餐。")
    private String content;
}
