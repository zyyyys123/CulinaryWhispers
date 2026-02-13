package com.zyyyys.culinarywhispers.module.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyyyys.culinarywhispers.module.ai.config.AiProperties;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatMessage;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatRequest;
import com.zyyyys.culinarywhispers.module.ai.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiProperties aiProperties;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    public AiChatResponse chat(AiChatRequest req) {
        String message = req == null ? "" : String.valueOf(req.getMessage() == null ? "" : req.getMessage()).trim();
        List<KnowledgeBaseService.KbChunk> chunks = knowledgeBaseService.retrieve(message);
        List<String> sources = new ArrayList<>();
        for (KnowledgeBaseService.KbChunk c : chunks) {
            sources.add(c.source());
        }

        boolean remoteEnabled = aiProperties.isEnabled() && aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank();
        if (!remoteEnabled) {
            AiChatResponse r = new AiChatResponse();
            r.setUsedRemoteModel(false);
            r.setSources(dedupe(sources));
            r.setReply(buildLocalReply(message, chunks));
            return r;
        }

        String reply = callOpenAi(message, req == null ? null : req.getHistory(), chunks);
        AiChatResponse r = new AiChatResponse();
        r.setUsedRemoteModel(true);
        r.setSources(dedupe(sources));
        r.setReply(reply);
        return r;
    }

    private String buildLocalReply(String message, List<KnowledgeBaseService.KbChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前未配置 AI API Key，已使用本地知识库进行回答占位。\n");
        if (message != null && !message.isBlank()) {
            sb.append("\n问题：").append(message).append("\n");
        }
        if (chunks == null || chunks.isEmpty()) {
            sb.append("\n未检索到匹配的知识库内容。你可以把规范/功能说明放到 ai 模块的 knowledge 目录下（.md/.txt），我会优先引用。");
            return sb.toString();
        }
        sb.append("\n参考要点：\n");
        int i = 1;
        for (KnowledgeBaseService.KbChunk c : chunks) {
            sb.append(i++).append(". ").append(trimTo(c.text(), 220)).append("\n");
        }
        sb.append("\n如果需要我用真实模型回答：在 application.yml 配置 culinary.ai.api-key（建议通过环境变量注入）。");
        return sb.toString().trim();
    }

    private String callOpenAi(String message, List<AiChatMessage> history, List<KnowledgeBaseService.KbChunk> chunks) {
        List<Map<String, String>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", buildSystemPrompt(chunks)));
        if (history != null) {
            for (AiChatMessage m : history) {
                if (m == null) continue;
                String role = m.getRole() == null ? "" : m.getRole().trim();
                String content = m.getContent() == null ? "" : m.getContent().trim();
                if (role.isBlank() || content.isBlank()) continue;
                if (!role.equals("user") && !role.equals("assistant")) continue;
                msgs.add(Map.of("role", role, "content", content));
            }
        }
        msgs.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "temperature", aiProperties.getTemperature(),
                "messages", msgs
        );

        String raw = aiRestClient.post()
                .uri(aiProperties.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey().trim())
                .body(body)
                .retrieve()
                .body(String.class);

        if (raw == null || raw.isBlank()) {
            return "AI 接口返回为空。";
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content != null && !content.isMissingNode()) {
                    String v = content.asText("");
                    if (!v.isBlank()) {
                        return v.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "AI 接口响应解析失败。";
    }

    private String buildSystemPrompt(List<KnowledgeBaseService.KbChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        String basePrompt = knowledgeBaseService.getSystemPrompt();
        if (basePrompt != null && !basePrompt.isBlank()) {
            sb.append(basePrompt.trim());
        } else {
            sb.append("你是 CulinaryWhispers（一个菜谱与社交平台）的 AI 助手。");
            sb.append("回答要求：中文；简洁；优先给可执行步骤；不确定时说明不确定。");
            sb.append("不得编造不存在的接口/页面/字段。");
        }

        if (chunks != null && !chunks.isEmpty()) {
            sb.append("\n\n以下为本地知识库片段（优先参考）：\n");
            int used = 0;
            for (KnowledgeBaseService.KbChunk c : chunks) {
                String t = c.text();
                if (t == null || t.isBlank()) continue;
                sb.append("\n[来源] ").append(c.source()).append("\n");
                sb.append(trimTo(t, 900)).append("\n");
                used += t.length();
                if (used >= aiProperties.getMaxContextChars()) break;
            }
        }
        return sb.toString();
    }

    private List<String> dedupe(List<String> in) {
        if (in == null || in.isEmpty()) return List.of();
        Set<String> s = new LinkedHashSet<>();
        for (String v : in) {
            if (v != null && !v.isBlank()) s.add(v);
        }
        return new ArrayList<>(s);
    }

    private String trimTo(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "...";
    }
}
