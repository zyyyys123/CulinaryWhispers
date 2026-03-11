package com.zyyyys.culinarywhispers.module.ai.controller;

import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatRequest;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatResponse;
import com.zyyyys.culinarywhispers.module.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI 聊天与知识库问答")
public class AiController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    @Operation(
            summary = "AI 对话",
            description = "输入本轮 message，可携带 history 作为上下文。配置 culinary.ai.api-key 后可使用真实模型。",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = AiChatRequest.class))
            )
    )
    public Result<AiChatResponse> chat(@org.springframework.web.bind.annotation.RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }
}
