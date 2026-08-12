package com.oyuki.referral.service;

import com.oyuki.referral.entity.Referral;
import com.oyuki.referral.enums.ReferralStatus;
import com.oyuki.referral.repository.ReferralRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.repository.UserRepository;
import com.oyuki.wallet.entity.SellerWallet;
import com.oyuki.wallet.entity.WalletTransaction;
import com.oyuki.wallet.enums.WalletTransactionDirection;
import com.oyuki.wallet.enums.WalletTransactionType;
import com.oyuki.wallet.repository.SellerWalletRepository;
import com.oyuki.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final SellerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Value("${app.referral.referrer-reward:500}")
    private BigDecimal referrerReward;

    @Value("${app.referral.referred-user-reward:0}")
    private BigDecimal referredUserReward;

    @Transactional
    public String ensureReferralCode(User user) {
        if (user.getReferralCode() != null && !user.getReferralCode().isBlank()) {
            return user.getReferralCode();
        }

        String base = buildBase(user.getFullName());
        String code;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 6).toUpperCase(Locale.ROOT);
            code = base + "-" + suffix;
        } while (userRepository.existsByReferralCodeIgnoreCase(code));

        user.setReferralCode(code);
        userRepository.save(user);
        return code;
    }

    @Transactional
    public void createPendingReferral(String suppliedCode, User referredUser) {
        if (suppliedCode == null || suppliedCode.isBlank()) return;

        String code = suppliedCode.trim().toUpperCase(Locale.ROOT);
        User referrer = userRepository.findByReferralCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        if (Objects.equals(referrer.getId(), referredUser.getId())) {
            throw new IllegalArgumentException("You cannot use your own referral code");
        }

        if (referralRepository.existsByReferredUser_Id(referredUser.getId())) return;

        referralRepository.save(Referral.builder()
                .referrer(referrer)
                .referredUser(referredUser)
                .referralCode(referrer.getReferralCode())
                .status(ReferralStatus.PENDING)
                .referrerReward(BigDecimal.ZERO)
                .referredUserReward(BigDecimal.ZERO)
                .build());
    }

    @Transactional
    public void rewardAfterOtpVerification(User referredUser) {
        Referral referral = referralRepository.findByReferredUser_Id(referredUser.getId())
                .orElse(null);
        if (referral == null || referral.getStatus() == ReferralStatus.REWARDED) return;
        if (referral.getStatus() == ReferralStatus.REJECTED || referral.getStatus() == ReferralStatus.CANCELLED) return;

        referral.setStatus(ReferralStatus.QUALIFIED);
        referral.setQualifiedAt(LocalDateTime.now());

        credit(referral.getReferrer(), referrerReward,
                "Referral reward for " + referredUser.getFullName(),
                "REFERRAL-" + referral.getId() + "-REFERRER",
                WalletTransactionType.REFERRAL_REWARD);

        if (referredUserReward.signum() > 0) {
            credit(referredUser, referredUserReward,
                    "Welcome referral reward",
                    "REFERRAL-" + referral.getId() + "-REFERRED",
                    WalletTransactionType.WELCOME_REWARD);
        }

        referral.setReferrerReward(referrerReward);
        referral.setReferredUserReward(referredUserReward);
        referral.setStatus(ReferralStatus.REWARDED);
        referral.setRewardedAt(LocalDateTime.now());
        referralRepository.save(referral);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyReferralSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String code = user.getReferralCode();
        List<Referral> referrals = referralRepository.findAllByReferrer_IdOrderByCreatedAtDesc(userId);

        long verified = referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.QUALIFIED || r.getStatus() == ReferralStatus.REWARDED)
                .count();

        BigDecimal totalEarned = referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.REWARDED)
                .map(Referral::getReferrerReward)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> history = referrals.stream().map(this::mapReferral).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("referralCode", code);
        result.put("totalInvited", referrals.size());
        result.put("verifiedInvited", verified);
        result.put("totalEarned", totalEarned);
        result.put("rewardPerVerifiedReferral", referrerReward);
        result.put("history", history);
        return result;
    }

    @Transactional
    public void backfillMissingReferralCodes() {
        for (User user : userRepository.findAll()) {
            if (user.getReferralCode() == null || user.getReferralCode().isBlank()) {
                ensureReferralCode(user);
            }
        }
    }

    private void credit(User user, BigDecimal amount, String description,
                        String reference, WalletTransactionType type) {
        if (amount == null || amount.signum() <= 0 || transactionRepository.existsByReference(reference)) return;

        SellerWallet wallet = walletRepository.findByUser_Id(user.getId())
                .orElseGet(() -> walletRepository.save(SellerWallet.builder().user(user).build()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);

        transactionRepository.save(WalletTransaction.builder()
                .user(user)
                .type(type)
                .direction(WalletTransactionDirection.CREDIT)
                .amount(amount)
                .description(description)
                .reference(reference)
                .status("COMPLETED")
                .build());
    }

    private Map<String, Object> mapReferral(Referral referral) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", referral.getId());
        item.put("referredName", referral.getReferredUser().getFullName());
        item.put("status", referral.getStatus());
        item.put("reward", referral.getReferrerReward());
        item.put("referrerReward", referral.getReferrerReward());
        item.put("createdAt", referral.getCreatedAt());
        item.put("qualifiedAt", referral.getQualifiedAt());
        item.put("rewardedAt", referral.getRewardedAt());
        return item;
    }

    private String buildBase(String fullName) {
        String normalized = fullName == null ? "OYUKI" : fullName.replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) normalized = "OYUKI";
        return normalized.substring(0, Math.min(8, normalized.length()));
    }
}
