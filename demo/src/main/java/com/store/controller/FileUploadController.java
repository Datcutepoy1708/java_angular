package com.store.controller;

import com.store.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@Tag(name = "File Upload", description = "File upload APIs for products, brands, and categories")
public class FileUploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".svg", ".gif");

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image file", description = "Upload image file (JPG, PNG, WEBP, SVG, GIF up to 5MB)")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file để tải lên"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Dung lượng file vượt quá giới hạn cho phép (tối đa 5MB)"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tên file không hợp lệ"));
        }

        String extension = "";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex >= 0) {
            extension = originalFilename.substring(extIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Định dạng file không được hỗ trợ. Vui lòng chọn ảnh JPG, PNG, WEBP, SVG hoặc GIF"));
        }

        try {
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String newFilename = UUID.randomUUID() + extension;
            Path targetLocation = uploadDir.resolve(newFilename).normalize();

            // Prevent path traversal
            if (!targetLocation.startsWith(uploadDir)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Đường dẫn lưu file không an toàn"));
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(newFilename)
                    .toUriString();

            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", newFilename);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tải ảnh lên thành công", data));
        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể lưu trữ file. Vui lòng thử lại"));
        }
    }
}
