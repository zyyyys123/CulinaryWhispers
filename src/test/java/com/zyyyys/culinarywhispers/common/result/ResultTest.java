package com.zyyyys.culinarywhispers.common.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSuccessSerialization() throws JsonProcessingException {
        Result<String> result = Result.success("test data");
        String json = objectMapper.writeValueAsString(result);

        // Verify JSON structure
        assertTrue(json.contains("\"code\":0"));
        assertTrue(json.contains("\"message\":\"操作成功\""));
        assertTrue(json.contains("\"data\":\"test data\""));
    }

    @Test
    void testErrorSerialization() throws JsonProcessingException {
        Result<Void> result = Result.error(500, "error msg");
        String json = objectMapper.writeValueAsString(result);

        assertTrue(json.contains("\"code\":500"));
        assertTrue(json.contains("\"message\":\"error msg\""));
        assertTrue(json.contains("\"data\":null"));
    }
}
