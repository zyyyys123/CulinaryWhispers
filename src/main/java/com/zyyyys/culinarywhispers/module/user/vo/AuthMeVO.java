package com.zyyyys.culinarywhispers.module.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(name = "AuthMeVO", description = "当前登录用户的认证信息与权限能力集")
public class AuthMeVO {
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "test_user")
    private String username;

    @Schema(description = "角色列表", example = "[\"USER\"]")
    private List<String> roles;

    @Schema(description = "权限码列表", example = "[]")
    private List<String> permissions;

    @Schema(description = "能力集（可扩展）")
    private Map<String, Object> capabilities;
}
