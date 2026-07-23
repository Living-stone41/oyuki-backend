package com.oyuki.common.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_FOLDERS =
            Set.of(
                    "profiles",
                    "covers",
                    "documents",
                    "products"
            );

    private static final Map<String, String> IMAGE_TYPES =
            Map.of(
                    "image/jpeg", ".jpg",
                    "image/png", ".png",
                    "image/webp", ".webp"
            );

    private static final Map<String, String> DOCUMENT_TYPES =
            Map.of(
                    "application/pdf", ".pdf",
                    "image/jpeg", ".jpg",
                    "image/png", ".png"
            );

    private final Path rootDirectory;

    public FileStorageService(
            @Value("${app.upload.root:uploads}") String uploadRoot
    ) {
        this.rootDirectory =
                Path.of(uploadRoot)
                        .toAbsolutePath()
                        .normalize();
    }

    @PostConstruct
    public void initializeFolders() {
        try {
            Files.createDirectories(rootDirectory);

            for (String folder : ALLOWED_FOLDERS) {
                Files.createDirectories(
                        rootDirectory.resolve(folder)
                );
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not initialize upload folders",
                    exception
            );
        }
    }

    public String storeImage(
            MultipartFile file,
            String folder
    ) {
        validateFolder(folder);
        validateFile(file);

        String contentType = file.getContentType();
        String extension = IMAGE_TYPES.get(contentType);

        if (extension == null) {
            throw new IllegalArgumentException(
                    "Only JPG, PNG and WebP images are allowed"
            );
        }

        String filename = saveFile(
                file,
                folder,
                extension
        );

        return "/uploads/" + folder + "/" + filename;
    }

    public String storeDocument(MultipartFile file) {
        validateFile(file);

        String contentType = file.getContentType();
        String extension = DOCUMENT_TYPES.get(contentType);

        if (extension == null) {
            throw new IllegalArgumentException(
                    "Only PDF, JPG and PNG documents are allowed"
            );
        }

        String filename = saveFile(
                file,
                "documents",
                extension
        );

        /*
         * Documents are accessed through an admin-protected endpoint.
         */
        return "/api/admin/files/documents/" + filename;
    }

    public Resource loadDocument(String filename) {
        try {
            String safeFilename =
                    Path.of(filename)
                            .getFileName()
                            .toString();

            Path documentPath = rootDirectory
                    .resolve("documents")
                    .resolve(safeFilename)
                    .normalize();

            Path documentFolder =
                    rootDirectory
                            .resolve("documents")
                            .normalize();

            if (!documentPath.startsWith(documentFolder)) {
                throw new IllegalArgumentException(
                        "Invalid document path"
                );
            }

            Resource resource =
                    new UrlResource(documentPath.toUri());

            if (!resource.exists() ||
                    !resource.isReadable()) {

                throw new IllegalArgumentException(
                        "Document was not found"
                );
            }

            return resource;

        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException(
                    "Invalid document path",
                    exception
            );
        }
    }

    private String saveFile(
            MultipartFile file,
            String folder,
            String extension
    ) {
        String filename =
                UUID.randomUUID() + extension;

        Path folderPath =
                rootDirectory
                        .resolve(folder)
                        .normalize();

        Path targetPath =
                folderPath
                        .resolve(filename)
                        .normalize();

        if (!targetPath.startsWith(folderPath)) {
            throw new IllegalArgumentException(
                    "Invalid file destination"
            );
        }

        try {
            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return filename;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not save uploaded file",
                    exception
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file"
            );
        }
    }

    private void validateFolder(String folder) {
        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new IllegalArgumentException(
                    "Invalid upload folder"
            );
        }
    }
}