package com.zyyyys.culinarywhispers.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "culinary.ai")
public class AiProperties {
    private boolean enabled = false;
    private String provider = "openai";
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1/chat/completions";
    private String model = "gpt-4o-mini";
    private double temperature = 0.3;
    private String knowledgePath = "docs2.0/knowledge";
    private int maxContextChars = 3500;
    private int maxChunks = 4;
}

