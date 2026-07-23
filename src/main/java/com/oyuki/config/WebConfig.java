package com.oyuki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadRoot;

    public WebConfig(
            @Value("${app.upload.root:uploads}") String uploadRoot
    ) {
        this.uploadRoot =
                Path.of(uploadRoot)
                        .toAbsolutePath()
                        .normalize();
    }

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {
        registry
                .addResourceHandler(
                        "/uploads/profiles/**"
                )
                .addResourceLocations(
                        uploadRoot
                                .resolve("profiles")
                                .toUri()
                                .toString()
                );

        registry
                .addResourceHandler(
                        "/uploads/covers/**"
                )
                .addResourceLocations(
                        uploadRoot
                                .resolve("covers")
                                .toUri()
                                .toString()
                );

        registry
                .addResourceHandler(
                        "/uploads/products/**"
                )
                .addResourceLocations(
                        uploadRoot
                                .resolve("products")
                                .toUri()
                                .toString()
                );
    }
}