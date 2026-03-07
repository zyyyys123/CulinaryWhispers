package com.zyyyys.culinarywhispers.module.file.controller;

import com.zyyyys.culinarywhispers.common.exception.BusinessException;
import com.zyyyys.culinarywhispers.common.result.Result;
import com.zyyyys.culinarywhispers.common.result.ResultCode;
import com.zyyyys.culinarywhispers.common.config.UploadProperties;
import com.zyyyys.culinarywhispers.module.file.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final UploadProperties uploadProperties;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        if (file.getSize() > uploadProperties.maxSizeBytes()) {
            throw new BusinessException(400, "文件过大（最大 " + uploadProperties.getMaxSizeMb() + "MB）");
        }
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "";
        Set<String> allowed = uploadProperties.allowedContentTypesSet();
        if (!allowed.isEmpty() && !allowed.contains(contentType)) {
            throw new BusinessException(400, "仅支持 JPG/PNG/WEBP/GIF 图片");
        }

        String ext = extFromContentType(contentType);
        String name = UUID.randomUUID().toString().replace("-", "") + ext;

        Path uploadDir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
        Path target = uploadDir.resolve(name);
        try {
            Files.createDirectories(uploadDir);
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(500, "上传失败");
        }

        UploadResponse resp = new UploadResponse();
        String prefix = uploadProperties.getPublicUrlPrefix();
        if (prefix == null) prefix = "/api/uploads/";
        if (!prefix.endsWith("/")) prefix = prefix + "/";
        resp.setUrl(prefix + name);
        resp.setName(name);
        resp.setSize(file.getSize());
        resp.setContentType(contentType);
        return Result.success(resp);
    }

    private String extFromContentType(String contentType) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("png")) return ".png";
        if (ct.contains("webp")) return ".webp";
        if (ct.contains("gif")) return ".gif";
        return ".jpg";
    }
}
