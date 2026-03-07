package com.zyyyys.culinarywhispers.module.ai.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@Schema(name = "AiChatRequest", description = "AI 对话请求")
public class AiChatRequest {
    @Schema(description = "本轮用户输入", example = "给我推荐一道适合新手的家常菜，并给出步骤。", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
    @Schema(description = "历史对话（用于上下文）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<AiChatMessage> history;
}
