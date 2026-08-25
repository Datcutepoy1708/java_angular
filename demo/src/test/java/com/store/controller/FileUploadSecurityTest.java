package com.store.controller;

import com.store.dto.response.ApiResponse;
import com.store.util.FileSecurityValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadSecurityTest {

    private FileUploadController fileUploadController;
    private FileSecurityValidator fileSecurityValidator;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        fileSecurityValidator = new FileSecurityValidator();
        fileUploadController = new FileUploadController(fileSecurityValidator);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should accept valid JPEG file with authentic Magic Bytes")
    void uploadValidJpeg_Success() {
        byte[] validJpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                validJpegContent
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).containsKey("url");
        assertThat(response.getBody().getData().get("filename")).endsWith(".jpg");
    }

    @Test
    @DisplayName("Should accept valid PNG file with authentic Magic Bytes")
    void uploadValidPng_Success() {
        byte[] validPngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "graphic.png",
                "image/png",
                validPngContent
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).containsKey("url");
        assertThat(response.getBody().getData().get("filename")).endsWith(".png");
    }

    @Test
    @DisplayName("Should REJECT spoofed file: PHP script disguised with .jpg extension (Magic Bytes check fails)")
    void uploadSpoofedPhpFile_RejectedByMagicBytes() {
        byte[] phpMalwareContent = "<?php system($_GET['cmd']); ?>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fakeJpgFile = new MockMultipartFile(
                "file",
                "shell.php.jpg",
                "image/jpeg",
                phpMalwareContent
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(fakeJpgFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("không khớp với phần mở rộng hình ảnh");
    }

    @Test
    @DisplayName("Should REJECT malicious SVG containing XSS or javascript payload")
    void uploadMaliciousSvg_Rejected() {
        String xssSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert('XSS')</script></svg>";
        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "icon.svg",
                "image/svg+xml",
                xssSvg.getBytes(StandardCharsets.UTF_8)
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(svgFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Should REJECT disallowed extensions (.exe, .sh, .jsp)")
    void uploadDisallowedExtension_Rejected() {
        byte[] content = "binary content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "program.exe",
                "application/octet-stream",
                content
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(exeFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Định dạng file không được hỗ trợ");
    }

    @Test
    @DisplayName("Should REJECT file exceeding 5MB limit")
    void uploadOversizedFile_Rejected() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge.jpg",
                "image/jpeg",
                largeContent
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(largeFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("vượt quá giới hạn");
    }

    @Test
    @DisplayName("Should REJECT empty file")
    void uploadEmptyFile_Rejected() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        ResponseEntity<ApiResponse<Map<String, String>>> response = fileUploadController.uploadFile(emptyFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
