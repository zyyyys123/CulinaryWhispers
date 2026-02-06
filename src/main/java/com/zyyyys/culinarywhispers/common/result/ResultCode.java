package com.zyyyys.culinarywhispers.common.result;

import lombok.Getter;

/**
 * 响应状态码
 * @author zyyyys
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "系统异常"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    VALIDATE_FAILED(400, "参数校验失败"),
    DATA_NOT_FOUND(404, "数据不存在"),
    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_EXIST(1003, "用户已存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
