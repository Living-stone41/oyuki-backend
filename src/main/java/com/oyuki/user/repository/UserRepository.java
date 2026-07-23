package com.oyuki.user.repository;

import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /*
     * Find a user using their email address.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /*
     * Find a user using their phone number.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /*
     * Used during login.
     *
     * Pass the login value into both parameters:
     *
     * findByEmailIgnoreCaseOrPhoneNumber(identifier, identifier)
     */
    Optional<User> findByEmailIgnoreCaseOrPhoneNumber(
            String email,
            String phoneNumber
    );

    /*
     * Check whether an email is already registered.
     */
    boolean existsByEmailIgnoreCase(String email);

    /*
     * Check whether a phone number is already registered.
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /*
     * Admin can retrieve users by role.
     */
    List<User> findAllByRole(Role role);

    /*
     * Admin can retrieve users by account status.
     */
    List<User> findAllByStatus(AccountStatus status);

    /*
     * Retrieve users matching both role and status.
     */
    List<User> findAllByRoleAndStatus(
            Role role,
            AccountStatus status
    );

    /*
     * Dashboard statistics.
     */
    long countByRole(Role role);

    long countByStatus(AccountStatus status);
    
}       