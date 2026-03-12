package com.zyyyys.culinarywhispers.common.security;

import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.utils.SecurityUtil;
import com.zyyyys.culinarywhispers.module.user.vo.AuthMeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "鉴权", description = "认证与权限能力集")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "返回当前登录用户的 roles/permissions/capabilities，用于前端动态路由与按钮级控制")
    public Result<AuthMeVO> me() {
        Long userId = SecurityUtil.getUserId();

        List<String> roles = UserContext.getRoles().stream().map(Enum::name).toList();
        List<String> permissions = new ArrayList<>();
        if (UserContext.isAdmin()) {
            permissions.add("*");
        }

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("isAdmin", UserContext.isAdmin());

        AuthMeVO vo = new AuthMeVO();
        vo.setUserId(userId);
        vo.setUsername(UserContext.getUsername());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        vo.setCapabilities(capabilities);
        return Result.success(vo);
    }
}
