package com.oyuki.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oyuki.admin.dto.AdminApplicationResponse;
import com.oyuki.admin.dto.RejectApplicationRequest;
import com.oyuki.seller.entity.SellerProfile;
import com.oyuki.seller.repository.SellerProfileRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminApprovalService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ObjectMapper objectMapper;

    /*
     * This defaults to the "uploads" folder in the project.
     *
     * You may override it in application.properties:
     *
     * app.upload.dir=/app/uploads
     */
    private final Path uploadRoot;

    public AdminApprovalService(
            UserRepository userRepository,
            SellerProfileRepository sellerProfileRepository,
            ObjectMapper objectMapper,
            @Value("${app.upload.root:uploads}") String uploadDirectory
    ) {
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
        this.objectMapper = objectMapper;

        this.uploadRoot = Paths.get(uploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    @Transactional(readOnly = true)
    public List<AdminApplicationResponse> getPendingApplications() {

        List<User> pendingSellers =
                userRepository.findAllByRoleAndStatus(
                        Role.SELLER,
                        AccountStatus.PENDING_APPROVAL
                );

        List<AdminApplicationResponse> applications =
                new ArrayList<>();

        pendingSellers
                .stream()
                .map(this::convertToResponse)
                .forEach(applications::add);

        applications.sort(
                Comparator.comparing(
                        AdminApplicationResponse::registeredAt,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        return applications;
    }

    @Transactional(readOnly = true)
    public AdminApplicationResponse getApplication(
            Long userId
    ) {

        User user = getSeller(userId);

        return convertToResponse(user);
    }

    @Transactional
    public AdminApplicationResponse approveApplication(
            Long userId
    ) {

        User user = getSeller(userId);

        validatePendingApplication(user);
        validateProfileCompleted(user);

        user.setStatus(AccountStatus.ACTIVE);
        user.setStatusReason(null);

        User savedUser =
                userRepository.save(user);

        return convertToResponse(savedUser);
    }

    @Transactional
    public AdminApplicationResponse rejectApplication(
            Long userId,
            RejectApplicationRequest request
    ) {

        User user = getSeller(userId);

        validatePendingApplication(user);

        if (
                request == null ||
                request.reason() == null ||
                request.reason().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "A rejection reason is required"
            );
        }

        user.setStatus(
                AccountStatus.REJECTED
        );

        user.setStatusReason(
                request.reason().trim()
        );

        /*
         * Invalidate JWTs issued before the rejection.
         */
        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        User savedUser =
                userRepository.save(user);

        return convertToResponse(savedUser);
    }

    /*
     * Downloads the seller's uploaded identification document.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadIdDocument(
            Long userId
    ) {

        User user =
                getSeller(userId);

        String documentUrl =
                getIdDocumentUrl(user);

        if (
                documentUrl == null ||
                documentUrl.isBlank()
        ) {
            throw new IllegalStateException(
                    "The seller has not uploaded an identification document"
            );
        }

        Path documentPath =
                resolveUploadedFile(documentUrl);

        if (
                !Files.exists(documentPath) ||
                !Files.isRegularFile(documentPath)
        ) {
            throw new IllegalStateException(
                    "The identification document could not be found on the server"
            );
        }

        Resource resource =
                new FileSystemResource(
                        documentPath
                );

        String contentType =
                detectContentType(documentPath);

        String filename =
                documentPath
                        .getFileName()
                        .toString();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                contentType
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentLength(
                        getFileSize(documentPath)
                )
                .body(resource);
    }

    /*
     * Downloads the complete seller application details as a JSON file.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadApplication(
            Long userId
    ) {

        User user =
                getSeller(userId);

        AdminApplicationResponse application =
                convertToResponse(user);

        byte[] content;

        try {

            String json =
                    objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(
                                    application
                            );

            content =
                    json.getBytes(
                            StandardCharsets.UTF_8
                    );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "The application file could not be generated",
                    exception
            );
        }

        String safeName =
                createSafeFilename(
                        application.fullName()
                );

        String filename =
                "oyuki-application-" +
                userId +
                "-" +
                safeName +
                ".json";

        ByteArrayResource resource =
                new ByteArrayResource(
                        content
                );

        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .contentLength(
                        content.length
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    private User getSeller(
            Long userId
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application was not found"
                                )
                        );

        if (
                user.getRole() != Role.SELLER
        ) {
            throw new IllegalStateException(
                    "This user is not a seller"
            );
        }

        return user;
    }

    private void validatePendingApplication(
            User user
    ) {

        if (
                user.getStatus() !=
                AccountStatus.PENDING_APPROVAL
        ) {

            throw new IllegalStateException(
                    "This application is not awaiting approval"
            );
        }
    }

    private void validateProfileCompleted(
            User user
    ) {

        SellerProfile profile =
                sellerProfileRepository
                        .findByUserId(
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "The seller must complete their profile before approval"
                                )
                        );

        requireApprovalFields(
                profile.getBusinessName(),
                profile.getAddressLine(),
                profile.getProfileImageUrl(),
                profile.getIdDocumentUrl()
        );
    }

    private void requireApprovalFields(
            String businessName,
            String addressLine,
            String profileImageUrl,
            String idDocumentUrl
    ) {

        if (
                businessName == null ||
                businessName.isBlank()
        ) {

            throw new IllegalStateException(
                    "A business name is required before approval"
            );
        }

        if (
                addressLine == null ||
                addressLine.isBlank()
        ) {

            throw new IllegalStateException(
                    "A business address is required before approval"
            );
        }

        if (
                profileImageUrl == null ||
                profileImageUrl.isBlank()
        ) {

            throw new IllegalStateException(
                    "A profile picture is required before approval"
            );
        }

        if (
                idDocumentUrl == null ||
                idDocumentUrl.isBlank()
        ) {

            throw new IllegalStateException(
                    "An identification document is required before approval"
            );
        }
    }

    private String getIdDocumentUrl(
            User user
    ) {

        SellerProfile profile =
                sellerProfileRepository
                        .findByUserId(
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Seller profile was not found"
                                )
                        );

        return profile.getIdDocumentUrl();
    }

    private Path resolveUploadedFile(
            String storedUrl
    ) {

        String cleanValue =
                storedUrl
                        .trim()
                        .replace("\\", "/");

        String filename =
                Path.of(cleanValue)
                        .getFileName()
                        .toString();

        Path documentFolder =
                uploadRoot
                        .resolve("documents")
                        .normalize();

        Path resolvedPath =
                documentFolder
                        .resolve(filename)
                        .normalize();

        if (!resolvedPath.startsWith(documentFolder)) {
            throw new IllegalStateException(
                    "Invalid document path"
            );
        }

        return resolvedPath;
    }

    private String detectContentType(
            Path path
    ) {

        try {

            String detected =
                    Files.probeContentType(
                            path
                    );

            if (
                    detected != null &&
                    !detected.isBlank()
            ) {
                return detected;
            }

        } catch (IOException ignored) {
            /*
             * Use the default type below.
             */
        }

        return MediaType
                .APPLICATION_OCTET_STREAM_VALUE;
    }

    private long getFileSize(
            Path path
    ) {

        try {

            return Files.size(path);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "The document size could not be determined",
                    exception
            );
        }
    }

    private String createSafeFilename(
            String value
    ) {

        if (
                value == null ||
                value.isBlank()
        ) {
            return "provider";
        }

        String safeValue =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9]+",
                                "-"
                        )
                        .replaceAll(
                                "^-+|-+$",
                                ""
                        );

        return safeValue.isBlank()
                ? "provider"
                : safeValue;
    }

    private AdminApplicationResponse convertToResponse(
            User user
    ) {

        if (
                user.getRole() ==
                Role.SELLER
        ) {

            return sellerProfileRepository
                    .findByUserId(
                            user.getId()
                    )
                    .map(
                            AdminApplicationResponse::fromSeller
                    )
                    .orElseGet(() ->
                            AdminApplicationResponse
                                    .incomplete(user)
                    );
        }

        throw new IllegalStateException(
                "Unsupported application role"
        );
    }
}