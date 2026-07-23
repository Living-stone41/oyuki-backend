package com.oyuki.admin.service;

import com.oyuki.admin.dto.AdminUserResponse;
import com.oyuki.admin.dto.AdminUserStatisticsResponse;
import com.oyuki.admin.dto.UpdateUserStatusRequest;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(
            Long adminId,
            Role role,
            AccountStatus status
    ) {
        getActiveAdmin(adminId);

        List<User> users;

        if (role != null && status != null) {
            users = userRepository.findAllByRoleAndStatus(
                    role,
                    status
            );
        } else if (role != null) {
            users = userRepository.findAllByRole(role);
        } else if (status != null) {
            users = userRepository.findAllByStatus(status);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .sorted(
                        Comparator.comparing(
                                User::getCreatedAt,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserStatisticsResponse getStatistics(
            Long adminId
    ) {
        getActiveAdmin(adminId);

        return new AdminUserStatisticsResponse(
                userRepository.count(),

                userRepository.countByRole(Role.CUSTOMER),
                userRepository.countByRole(Role.SELLER),
                userRepository.countByRole(Role.KITCHEN),
                userRepository.countByRole(Role.RIDER),
                userRepository.countByRole(Role.ADMIN),

                userRepository.countByStatus(
                        AccountStatus.PENDING_VERIFICATION
                ),
                userRepository.countByStatus(
                        AccountStatus.PENDING_APPROVAL
                ),
                userRepository.countByStatus(
                        AccountStatus.ACTIVE
                ),
                userRepository.countByStatus(
                        AccountStatus.REJECTED
                ),
                userRepository.countByStatus(
                        AccountStatus.SUSPENDED
                ),
                userRepository.countByStatus(
                        AccountStatus.DISABLED
                )
        );
    }

    @Transactional
    public AdminUserResponse updateStatus(
            Long adminId,
            Long userId,
            UpdateUserStatusRequest request
    ) {
        User admin = getActiveAdmin(adminId);

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User account not found"
                        )
                );

        if (user.getId().equals(admin.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot change your own account status here"
            );
        }

        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot be changed here"
            );
        }

        String reason =
                request.reason() == null
                        || request.reason().isBlank()
                        ? null
                        : request.reason().trim();

        if (
                request.status() != AccountStatus.ACTIVE
                        && reason == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a reason for this account status"
            );
        }

        user.setStatus(request.status());
        user.setStatusReason(
                request.status() == AccountStatus.ACTIVE
                        ? null
                        : reason
        );

        /*
         * Invalidate any JWT already issued to the user.
         */
        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        return AdminUserResponse.from(
                userRepository.save(user)
        );
    }

    private User getActiveAdmin(
            Long adminId
    ) {
        User admin = userRepository
                .findById(adminId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Administrator account not found"
                        )
                );

        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can manage users"
            );
        }

        if (admin.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator account is not active"
            );
        }

        return admin;
    }
}
