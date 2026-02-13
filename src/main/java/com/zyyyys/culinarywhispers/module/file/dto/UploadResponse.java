package com.zyyyys.culinarywhispers.module.file.dto;

import lombok.Data;

@Data
public class UploadResponse {
    private String url;
    private String name;
    private long size;
    private String contentType;
}

