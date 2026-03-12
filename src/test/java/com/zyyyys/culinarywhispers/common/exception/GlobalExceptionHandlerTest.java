package com.zyyyys.culinarywhispers.common.exception;

import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @Test
    void handleBusinessException_returnsErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        ResponseEntity<Result<String>> resp = handler.handleBusinessException(new BusinessException(ResultCode.FORBIDDEN), req);
        assertEquals(ResultCode.FORBIDDEN.getCode(), resp.getBody().getCode());
        assertNotNull(resp.getBody().getRequestId());
        assertNotNull(resp.getBody().getTimestamp());
    }

    @Test
    void handleException_returnsGenericError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        ResponseEntity<Result<String>> resp = handler.handleException(new RuntimeException("x"), req);
        assertEquals(ResultCode.ERROR.getCode(), resp.getBody().getCode());
        assertNotNull(resp.getBody().getRequestId());
    }
}
