package com.zyyyys.culinarywhispers.common.log.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.log.dto.JsonLogCaptureDTO;
import com.zyyyys.culinarywhispers.common.log.service.JsonLogCaptureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 日志采集默认实现
 * 说明：输出为 JSON Line，便于后续对接 ELK/ClickHouse/数据仓库等。
 */
@Service
@RequiredArgsConstructor
public class JsonLogCaptureServiceImpl implements JsonLogCaptureService {
    private static final Logger JSON_LOGGER = LoggerFactory.getLogger("json-capture");

    private final ObjectMapper objectMapper;

    @Override
    public void capture(JsonLogCaptureDTO dto, HttpServletRequest request) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", safeTrim(dto.getEventType()));
        event.put("level", normalizeLevel(dto.getLevel()));
        event.put("message", safeTrim(dto.getMessage()));
        event.put("timestamp", dto.getTimestamp());
        event.put("serverTime", Instant.now().toEpochMilli());
        event.put("traceId", safeTrim(dto.getTraceId()));
        event.put("tags", dto.getTags());
        event.put("context", dto.getContext());
        event.put("stack", dto.getStack());

        // 关联后端上下文（注意：不记录敏感头信息）
        event.put("userId", UserContext.getUserId());
        if (request != null) {
            event.put("method", request.getMethod());
            event.put("path", request.getRequestURI());
            event.put("remoteAddr", request.getRemoteAddr());
            event.put("userAgent", request.getHeader("User-Agent"));
        }

        try {
            JSON_LOGGER.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            // 采集接口本身不要抛异常，避免“日志上报”反过来影响业务
            JSON_LOGGER.error("{\"eventType\":\"log_capture_error\",\"message\":\"serialize_failed\"}", e);
        }
    }

    private String normalizeLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return "INFO";
        }
        String l = level.trim().toUpperCase();
        if ("DEBUG".equals(l) || "INFO".equals(l) || "WARN".equals(l) || "ERROR".equals(l)) {
            return l;
        }
        return "INFO";
    }

    private String safeTrim(String v) {
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}

