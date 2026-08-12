package com.oyuki.wallet.controller;

import com.oyuki.user.repository.UserRepository;
<<<<<<< HEAD
=======
import com.oyuki.user.entity.User;
>>>>>>> 1f72347 (Update Oyuki backend)
import com.oyuki.referral.service.ReferralService;
import com.oyuki.wallet.entity.SellerWallet;
import com.oyuki.wallet.entity.WalletTransaction;
import com.oyuki.wallet.entity.WithdrawalRequest;
import com.oyuki.wallet.enums.WalletTransactionType;
import com.oyuki.wallet.enums.WithdrawalStatus;
import com.oyuki.wallet.repository.SellerWalletRepository;
import com.oyuki.wallet.repository.WalletTransactionRepository;
import com.oyuki.wallet.repository.WithdrawalRequestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private final SellerWalletRepository wallets;
    private final WithdrawalRequestRepository withdrawals;
    private final WalletTransactionRepository transactions;
    private final UserRepository users;
    private final ReferralService referralService;

    public WalletController(SellerWalletRepository wallets,
                            WithdrawalRequestRepository withdrawals,
                            WalletTransactionRepository transactions,
                            UserRepository users,
                            ReferralService referralService) {
        this.wallets = wallets;
        this.withdrawals = withdrawals;
        this.transactions = transactions;
        this.users = users;
        this.referralService = referralService;
    }

    @GetMapping
    public Map<String, Object> wallet(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return mapWallet(get(userId), userId);
    }

    @PutMapping("/bank")
    public Map<String, Object> bank(Authentication authentication,
                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
        SellerWallet wallet = get(userId);
        wallet.setBankName(body.get("bankName"));
        wallet.setAccountNumber(body.get("accountNumber"));
        wallet.setAccountName(body.get("accountName"));
        return mapWallet(wallets.save(wallet), userId);
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<?> withdraw(Authentication authentication,
                                      @RequestBody Map<String, String> body) {
        Long userId = (Long) authentication.getPrincipal();
<<<<<<< HEAD
        referralService.assertReferralWithdrawalEligible(userId);
=======
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        long qualified = referralService.qualifiedCountForWithdrawal(user);
        int minimum = referralService.minimumWithdrawalReferrals();
        if (qualified < minimum) {
            throw new IllegalStateException("You need " + (minimum - qualified)
                    + " more qualified referrals before you can withdraw referral earnings");
        }
>>>>>>> 1f72347 (Update Oyuki backend)
        SellerWallet wallet = get(userId);
        BigDecimal amount = new BigDecimal(body.get("amount"));
        if (amount.signum() <= 0 || wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }
        if (wallet.getBankName() == null || wallet.getAccountNumber() == null || wallet.getAccountName() == null) {
            throw new IllegalArgumentException("Add your verified bank details before withdrawing");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setPendingBalance(wallet.getPendingBalance().add(amount));
        wallets.save(wallet);

        WithdrawalRequest request = WithdrawalRequest.builder()
                .seller(wallet.getUser())
                .amount(amount)
                .bankName(wallet.getBankName())
                .accountNumber(wallet.getAccountNumber())
                .accountName(wallet.getAccountName())
                .build();
        return ResponseEntity.status(201).body(mapWithdrawal(withdrawals.save(request)));
    }

    @GetMapping("/withdrawals/my")
    public List<Map<String, Object>> mine(Authentication authentication) {
        return withdrawals.findAllBySeller_IdOrderByCreatedAtDesc((Long) authentication.getPrincipal())
                .stream().map(this::mapWithdrawal).toList();
    }

    @GetMapping("/admin/withdrawals")
    public List<Map<String, Object>> all() {
        return withdrawals.findAllByOrderByCreatedAtDesc().stream().map(this::mapWithdrawal).toList();
    }

    @PatchMapping("/admin/withdrawals/{id}")
    public Map<String, Object> process(@PathVariable Long id,
                                       @RequestBody Map<String, String> body) {
        WithdrawalRequest request = withdrawals.findById(id).orElseThrow();
        WithdrawalStatus status = WithdrawalStatus.valueOf(body.get("status"));
        request.setStatus(status);
        request.setAdminNote(body.get("adminNote"));
        request.setProcessedAt(LocalDateTime.now());

        SellerWallet wallet = get(request.getSeller().getId());
        if (status == WithdrawalStatus.REJECTED) {
            wallet.setPendingBalance(wallet.getPendingBalance().subtract(request.getAmount()));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
            wallets.save(wallet);
        } else if (status == WithdrawalStatus.PAID) {
            wallet.setPendingBalance(wallet.getPendingBalance().subtract(request.getAmount()));
            wallets.save(wallet);
        }
        return mapWithdrawal(withdrawals.save(request));
    }

    private SellerWallet get(Long userId) {
        return wallets.findByUser_Id(userId)
                .orElseGet(() -> wallets.save(SellerWallet.builder()
                        .user(users.findById(userId).orElseThrow())
                        .build()));
    }

    private Map<String, Object> mapWallet(SellerWallet wallet, Long userId) {
        List<WalletTransaction> transactionList = transactions.findTop50ByUser_IdOrderByCreatedAtDesc(userId);
        BigDecimal referralEarnings = transactionList.stream()
                .filter(t -> t.getType() == WalletTransactionType.REFERRAL_REWARD)
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("availableBalance", wallet.getAvailableBalance());
        result.put("pendingBalance", wallet.getPendingBalance());
        result.put("referralEarnings", referralEarnings);
        result.put("bankName", wallet.getBankName());
        result.put("accountNumber", wallet.getAccountNumber());
        result.put("accountName", wallet.getAccountName());
<<<<<<< HEAD
        Map<String, Object> referralSummary = referralService.getMyReferralSummary(userId);
        result.put("qualifiedReferrals", referralSummary.get("qualifiedReferrals"));
        result.put("minimumWithdrawalReferrals", referralSummary.get("minimumWithdrawalReferrals"));
        result.put("remainingForWithdrawal", referralSummary.get("remainingForWithdrawal"));
        result.put("withdrawalEligible", referralSummary.get("withdrawalEligible"));
        result.put("referrerType", referralSummary.get("referrerType"));
=======
        User user = users.findById(userId).orElseThrow();
        long qualified = referralService.qualifiedCountForWithdrawal(user);
        int minimum = referralService.minimumWithdrawalReferrals();
        result.put("qualifiedReferralCount", qualified);
        result.put("minimumWithdrawalReferrals", minimum);
        result.put("remainingForWithdrawal", Math.max(0, minimum - qualified));
        result.put("withdrawalEligible", qualified >= minimum);
>>>>>>> 1f72347 (Update Oyuki backend)
        result.put("transactions", transactionList.stream().map(this::mapTransaction).toList());
        return result;
    }

    private Map<String, Object> mapTransaction(WalletTransaction transaction) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", transaction.getId());
        item.put("type", transaction.getType());
        item.put("direction", transaction.getDirection());
        item.put("amount", transaction.getAmount());
        item.put("description", transaction.getDescription());
        item.put("status", transaction.getStatus());
        item.put("createdAt", transaction.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapWithdrawal(WithdrawalRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", request.getId());
        result.put("sellerName", request.getSeller().getFullName());
        result.put("amount", request.getAmount());
        result.put("bankName", request.getBankName());
        result.put("accountNumber", request.getAccountNumber());
        result.put("accountName", request.getAccountName());
        result.put("status", request.getStatus());
        result.put("adminNote", request.getAdminNote());
        result.put("createdAt", request.getCreatedAt());
        return result;
    }
}
