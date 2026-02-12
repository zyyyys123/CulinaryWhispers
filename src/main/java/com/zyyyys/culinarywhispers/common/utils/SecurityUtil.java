package com.zyyyys.culinarywhispers.common.utils;

import com.zyyyys.culinarywhispers.common.context.UserContext;
import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.ResultCode;

/**
 * 安全工具类
 * @author zyyyys
 */
public class SecurityUtil {
    
    /**
     * 获取当前登录用户ID
     * @return userId
     */
    public static Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
