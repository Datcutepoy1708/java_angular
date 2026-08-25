package com.store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rootUploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        Path demoUploadDir = Paths.get("demo", "uploads").toAbsolutePath().normalize();
        Path parentUploadDir = Paths.get("..", "uploads").toAbsolutePath().normalize();

        String rootPath = formatPath(rootUploadDir);
        String demoPath = formatPath(demoUploadDir);
        String parentPath = formatPath(parentUploadDir);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        rootPath,
                        demoPath,
                        parentPath,
                        "file:uploads/",
                        "file:demo/uploads/",
                        "file:../uploads/",
                        "file:../demo/uploads/"
                );
    }

    private String formatPath(Path path) {
        String uri = path.toUri().toString();
        return uri.endsWith("/") ? uri : uri + "/";
    }
}
