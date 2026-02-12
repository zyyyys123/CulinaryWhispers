package com.zyyyys.culinarywhispers.common.exception;

import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void handleBusinessException_returnsErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Result<String> res = handler.handleBusinessException(new BusinessException(ResultCode.FORBIDDEN));
        assertEquals(ResultCode.FORBIDDEN.getCode(), res.getCode());
    }

    @Test
    void handleException_returnsGenericError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Result<String> res = handler.handleException(new RuntimeException("x"));
        assertEquals(ResultCode.ERROR.getCode(), res.getCode());
    }
}
