package com.oyuki.auth.repository;

import com.oyuki.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken>
    findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    List<PasswordResetToken>
    findAllByUserIdAndUsedFalse(Long userId);
}