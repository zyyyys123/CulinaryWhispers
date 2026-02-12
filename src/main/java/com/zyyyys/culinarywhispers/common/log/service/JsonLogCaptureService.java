package com.zyyyys.culinarywhispers.common.log.service;

import com.zyyyys.culinarywhispers.common.log.dto.JsonLogCaptureDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 结构化日志采集服务
 */
public interface JsonLogCaptureService {
    /**
     * 采集一条结构化日志（JSON Line）
     */
    void capture(JsonLogCaptureDTO dto, HttpServletRequest request);
}

