package com.zyyyys.culinarywhispers.common.log.controller;

import com.zyyyys.culinarywhispers.common.log.dto.JsonLogCaptureDTO;
import com.zyyyys.culinarywhispers.common.log.service.JsonLogCaptureService;
import com.zyyyys.culinarywhispers.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志采集接口
 * 场景：前端错误上报、埋点上报、性能上报等。
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogCaptureController {
    private final JsonLogCaptureService jsonLogCaptureService;

    @PostMapping("/capture")
    public Result<Void> capture(@RequestBody @Valid JsonLogCaptureDTO dto, HttpServletRequest request) {
        jsonLogCaptureService.capture(dto, request);
        return Result.success();
    }
}

