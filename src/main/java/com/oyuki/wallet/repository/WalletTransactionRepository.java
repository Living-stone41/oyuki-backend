package com.oyuki.wallet.repository;

import com.oyuki.wallet.entity.WalletTransaction;
import com.oyuki.wallet.enums.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findTop50ByUser_IdOrderByCreatedAtDesc(Long userId);
    boolean existsByReference(String reference);
}
