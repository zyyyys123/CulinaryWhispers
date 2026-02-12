package com.zyyyys.culinarywhispers.common.log.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 前端/客户端日志上报 DTO
 * 说明：使用 JsonNode 承接 context，允许透传任意 JSON 结构，便于统一采集与检索。
 */
@Data
public class JsonLogCaptureDTO {
    /**
     * 事件类型（例如：ui_error / api_error / performance / user_action）
     */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;

    /**
     * 日志级别（INFO/WARN/ERROR）
     */
    private String level;

    /**
     * 日志正文
     */
    @NotBlank(message = "message 不能为空")
    private String message;

    /**
     * 客户端时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 关联链路追踪ID（可选）
     */
    private String traceId;

    /**
     * 业务标签（可选）
     */
    private Map<String, String> tags;

    /**
     * 扩展上下文（可选，任意 JSON）
     */
    private JsonNode context;

    /**
     * 异常堆栈（可选）
     */
    private String stack;
}

