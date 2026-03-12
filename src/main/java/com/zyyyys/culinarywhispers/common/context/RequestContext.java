package com.zyyyys.culinarywhispers.common.context;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public class RequestContext {
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String ATTR_REQUEST_ID = "cw.requestId";
    public static final String ATTR_TRACE_ID = "cw.traceId";

    public static String ensureRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTR_REQUEST_ID);
        if (existing instanceof String s && !s.isBlank()) {
            return s;
        }
        String fromHeader = request.getHeader(HEADER_REQUEST_ID);
        String v = (fromHeader != null && !fromHeader.isBlank())
                ? fromHeader.trim()
                : UUID.randomUUID().toString().replace("-", "");
        request.setAttribute(ATTR_REQUEST_ID, v);
        return v;
    }

    public static String ensureTraceId(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTR_TRACE_ID);
        if (existing instanceof String s && !s.isBlank()) {
            return s;
        }
        String fromHeader = request.getHeader(HEADER_TRACE_ID);
        String v = (fromHeader != null && !fromHeader.isBlank())
                ? fromHeader.trim()
                : ensureRequestId(request);
        request.setAttribute(ATTR_TRACE_ID, v);
        return v;
    }
}
