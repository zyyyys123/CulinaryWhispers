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
            // 开发阶段，为了方便调试，如果没有设置上下文，暂时返回默认ID 1L
            // 在生产环境或完整实现了拦截器后，应抛出 UNAUTHORIZED 异常
            // throw new BusinessException(ResultCode.UNAUTHORIZED);
            return 1L; 
        }
        return userId;
    }
}
