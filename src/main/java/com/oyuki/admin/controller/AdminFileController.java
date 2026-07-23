package com.oyuki.admin.controller;

import com.oyuki.common.storage.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {

    private final FileStorageService fileStorageService;

    public AdminFileController(
            FileStorageService fileStorageService
    ) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/documents/{filename:.+}")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable String filename
    ) throws IOException {

        Resource resource =
                fileStorageService.loadDocument(filename);

        String contentType = "application/octet-stream";

        if (resource.getFile().toPath() != null) {
            String detectedType =
                    java.nio.file.Files.probeContentType(
                            resource.getFile().toPath()
                    );

            if (detectedType != null) {
                contentType = detectedType;
            }
        }

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(contentType)
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        resource.getFilename(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(resource);
    }
}