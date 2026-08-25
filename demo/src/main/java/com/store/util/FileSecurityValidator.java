package com.store.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class FileSecurityValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp", ".svg", ".gif");

    // Magic bytes signatures
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87A_MAGIC = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF89A_MAGIC = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF_MAGIC = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MAGIC = "WEBP".getBytes(StandardCharsets.US_ASCII);

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file để tải lên");
        }

        // 1. Validate File Size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Dung lượng file vượt quá giới hạn cho phép (tối đa 5MB)");
        }

        // 2. Validate Extension Whitelist
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }

        String extension = "";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex >= 0) {
            extension = originalFilename.substring(extIndex).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Vui lòng chọn ảnh JPG, PNG, WEBP, SVG hoặc GIF");
        }

        // 3. Validate Magic Bytes (Header Signature) to prevent extension spoofing (e.g. malware.php renamed to malware.jpg)
        try {
            byte[] header = new byte[32];
            int bytesRead;
            try (InputStream is = file.getInputStream()) {
                bytesRead = is.read(header);
            }

            if (bytesRead < 4) {
                throw new IllegalArgumentException("Nội dung file không hợp lệ hoặc bị hỏng");
            }

            boolean isValidSignature = switch (extension) {
                case ".jpg", ".jpeg" -> matchesPrefix(header, JPEG_MAGIC);
                case ".png" -> matchesPrefix(header, PNG_MAGIC);
                case ".gif" -> matchesPrefix(header, GIF87A_MAGIC) || matchesPrefix(header, GIF89A_MAGIC);
                case ".webp" -> isWebpSignature(header, bytesRead);
                case ".svg" -> isSvgContent(file);
                default -> false;
            };

            if (!isValidSignature) {
                log.warn("Security Alert: File '{}' with extension '{}' failed Magic Bytes signature validation!",
                        originalFilename, extension);
                throw new IllegalArgumentException("Nội dung file thực tế không khớp với phần mở rộng hình ảnh hợp lệ (Phát hiện giả mạo định dạng)");
            }

        } catch (IOException e) {
            log.error("Failed to inspect file magic bytes", e);
            throw new IllegalArgumentException("Không thể xác thực cấu trúc file tải lên");
        }
    }

    private boolean matchesPrefix(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebpSignature(byte[] data, int bytesRead) {
        // RIFF (4 bytes) + File size (4 bytes) + WEBP (4 bytes)
        if (bytesRead < 12) {
            return false;
        }
        return matchesPrefix(data, RIFF_MAGIC) &&
                data[8] == WEBP_MAGIC[0] &&
                data[9] == WEBP_MAGIC[1] &&
                data[10] == WEBP_MAGIC[2] &&
                data[11] == WEBP_MAGIC[3];
    }

    private boolean isSvgContent(MultipartFile file) throws IOException {
        // SVG is XML text; inspect first 1024 bytes for <svg and ensure no dangerous script tags
        byte[] buffer = new byte[1024];
        int read;
        try (InputStream is = file.getInputStream()) {
            read = is.read(buffer);
        }
        if (read <= 0) {
            return false;
        }

        String content = new String(Arrays.copyOf(buffer, read), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (content.contains("<script") || content.contains("javascript:") || content.contains("onload=")) {
            log.warn("Security Alert: SVG file contains potentially malicious executable script tags");
            return false;
        }
        return content.contains("<svg") || content.contains("<?xml");
    }
}
