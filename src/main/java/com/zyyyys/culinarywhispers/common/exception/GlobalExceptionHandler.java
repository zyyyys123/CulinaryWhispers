package com.zyyyys.culinarywhispers.common.exception;

import com.zyyyys.culinarywhispers.common.context.RequestContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Objects;

/**
 * 全局异常处理
 * @author zyyyys
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<String>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        int code = e.getCode() == null ? ResultCode.ERROR.getCode() : e.getCode();
        HttpStatus status = mapToHttpStatus(code);
        return buildError(status, code, e.getMessage(), request);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ResultCode.ERROR.getCode(), "系统繁忙，请稍后重试", request);
    }

    /**
     * 处理方法参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String message = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        return buildError(HttpStatus.BAD_REQUEST, ResultCode.VALIDATE_FAILED.getCode(), message, request);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<String>> handleBindException(BindException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String message = Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage();
        return buildError(HttpStatus.BAD_REQUEST, ResultCode.VALIDATE_FAILED.getCode(), message, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<String>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ResultCode.VALIDATE_FAILED.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<String>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ResultCode.VALIDATE_FAILED.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ResultCode.VALIDATE_FAILED.getCode(), "请求体解析失败", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<String>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED.value(), e.getMessage(), request);
    }

    private ResponseEntity<Result<String>> buildError(HttpStatus status, Integer code, String message, HttpServletRequest request) {
        Result<String> result = Result.error(code, message);
        result.setRequestId(RequestContext.ensureRequestId(request));
        result.setTraceId(RequestContext.ensureTraceId(request));
        result.setTimestamp(System.currentTimeMillis());
        result.setPath(request.getRequestURI());
        HttpHeaders headers = new HttpHeaders();
        headers.set(RequestContext.HEADER_REQUEST_ID, result.getRequestId());
        headers.set(RequestContext.HEADER_TRACE_ID, result.getTraceId());
        return ResponseEntity.status(status).headers(headers).body(result);
    }

    private HttpStatus mapToHttpStatus(int code) {
        if (code >= 200 && code <= 599) {
            try {
                return HttpStatus.valueOf(code);
            } catch (Exception ignored) {
                return HttpStatus.BAD_REQUEST;
            }
        }
        return HttpStatus.BAD_REQUEST;
    }
}
