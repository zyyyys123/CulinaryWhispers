package com.zyyyys.culinarywhispers.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@ConfigurationProperties(prefix = "cw.upload")
public class UploadProperties {
    private String dir = "uploads";
    private int maxSizeMb = 3;
    private int cacheSeconds = 3600;
    private String allowedContentTypes = "image/jpeg,image/png,image/webp,image/gif";
    private String publicUrlPrefix = "/api/uploads/";

    public long maxSizeBytes() {
        return (long) maxSizeMb * 1024 * 1024;
    }

    public Set<String> allowedContentTypesSet() {
        String v = allowedContentTypes == null ? "" : allowedContentTypes;
        return Arrays.stream(v.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

