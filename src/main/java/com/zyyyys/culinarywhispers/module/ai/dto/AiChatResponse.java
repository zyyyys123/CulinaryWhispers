package com.zyyyys.culinarywhispers.module.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatResponse {
    private String reply;
    private List<String> sources;
    private boolean usedRemoteModel;
}

