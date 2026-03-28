package com.campus.activity.common.controller;

import com.campus.activity.common.ApiException;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FileController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/api/v1/admin/files/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "file is required");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String name = ts + "_" + UUID.randomUUID().toString().replace("-", "");
        if (ext != null && !ext.isBlank()) {
            name = name + "." + ext;
        }
        
        // 使用绝对路径，避免由于工作目录不确定导致的问题
        File dir = new File(System.getProperty("user.dir"), UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File dest = new File(dir, name);
        try (var is = file.getInputStream()) {
            // Spring Boot 中推荐使用 Files.copy 或者使用绝对路径的 transferTo
            Files.copy(is, dest.toPath());
        } catch (IOException e) {
            // 记录异常便于后续排查（如果有日志查看能力）
            throw new ApiException(500, HttpStatus.INTERNAL_SERVER_ERROR, "upload failed: " + e.getMessage());
        }
        return Result.success("/api/v1/files/" + name);
    }

    @GetMapping("/api/v1/files/{name}")
    public ResponseEntity<Resource> get(@PathVariable String name) {
        File f = new File(new File(System.getProperty("user.dir"), UPLOAD_DIR), name);
        if (!f.exists() || !f.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        MediaType mt = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String ct = Files.probeContentType(f.toPath());
            if (ct != null) {
                mt = MediaType.parseMediaType(ct);
            }
        } catch (IOException ignored) {
        }
        Resource res = new FileSystemResource(f);
        return ResponseEntity.ok()
                .contentType(mt)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(res);
    }
}

