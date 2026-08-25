package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.util.FileSecurityValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "File Upload", description = "Secure file upload APIs restricted to authorized administrators and staff")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileSecurityValidator fileSecurityValidator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Upload an image file",
            description = "Upload image file (JPG, PNG, WEBP, SVG, GIF up to 5MB). Enforces strict Magic Bytes header validation, UUID renaming, and admin/staff authorization."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        // 1. Strict Security Validation (Size, Extension, and Magic Bytes Header Inspection)
        try {
            fileSecurityValidator.validateImageFile(file);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage()));
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int extIndex = originalFilename != null ? originalFilename.lastIndexOf('.') : -1;
        if (extIndex >= 0) {
            extension = originalFilename.substring(extIndex).toLowerCase(Locale.ROOT);
        }

        try {
            // 2. Storage Directory Resolution
            Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 3. Unpredictable Randomized Filename (UUID)
            String newFilename = UUID.randomUUID() + extension;
            Path targetLocation = uploadDir.resolve(newFilename).normalize();

            // 4. Anti-Path Traversal Defense
            if (!targetLocation.startsWith(uploadDir)) {
                log.error("Security Alert: Path traversal attempt detected with filename: {}", originalFilename);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Đường dẫn lưu file không an toàn"));
            }

            // 5. Atomic Stream Copy
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

            log.info("File uploaded successfully: original='{}', stored='{}', size={} bytes",
                    originalFilename, newFilename, file.getSize());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tải ảnh lên thành công", data));
        } catch (IOException ex) {
            log.error("Failed to store file securely", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Không thể lưu trữ file. Vui lòng thử lại"));
        }
    }
}
