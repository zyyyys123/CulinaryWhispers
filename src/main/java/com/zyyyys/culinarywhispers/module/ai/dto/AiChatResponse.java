package com.zyyyys.culinarywhispers.module.ai.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@Schema(name = "AiChatResponse", description = "AI 对话响应")
public class AiChatResponse {
    @Schema(description = "AI 回复内容", example = "推荐：番茄炒蛋。步骤：1) ...")
    private String reply;
    @Schema(description = "命中的知识库来源（文件名/片段）", example = "[\"docs2.0/knowledge/xxx.md#L10-L30\"]")
    private List<String> sources;
    @Schema(description = "是否调用了远端模型（否则为本地规则/知识库回答）", example = "false")
    private boolean usedRemoteModel;
}
