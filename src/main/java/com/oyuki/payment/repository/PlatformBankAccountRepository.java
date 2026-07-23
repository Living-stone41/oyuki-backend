package com.oyuki.payment.repository;

import com.oyuki.payment.entity.PlatformBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformBankAccountRepository
        extends JpaRepository<PlatformBankAccount, Long> {

    Optional<PlatformBankAccount>
    findFirstByActiveTrueOrderByUpdatedAtDesc();

    List<PlatformBankAccount>
    findAllByOrderByUpdatedAtDesc();

    List<PlatformBankAccount>
    findAllByActiveTrue();
}