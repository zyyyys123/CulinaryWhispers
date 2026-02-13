package com.zyyyys.culinarywhispers.module.ai.controller;

import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatRequest;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatResponse;
import com.zyyyys.culinarywhispers.module.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }
}

