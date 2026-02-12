package com.zyyyys.culinarywhispers.common.log.controller;

import com.zyyyys.culinarywhispers.common.log.dto.JsonLogCaptureDTO;
import com.zyyyys.culinarywhispers.common.log.service.JsonLogCaptureService;
import com.zyyyys.culinarywhispers.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LogCaptureControllerTest {

    @Test
    void capture_callsServiceAndReturnsSuccess() {
        JsonLogCaptureService service = mock(JsonLogCaptureService.class);
        LogCaptureController controller = new LogCaptureController(service);

        JsonLogCaptureDTO dto = new JsonLogCaptureDTO();
        dto.setEventType("ui_error");
        dto.setMessage("something happened");

        Result<Void> result = controller.capture(dto, null);
        assertEquals(0, result.getCode());
        verify(service, times(1)).capture(eq(dto), isNull());
    }
}

