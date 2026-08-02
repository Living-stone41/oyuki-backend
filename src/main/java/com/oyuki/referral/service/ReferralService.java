package com.oyuki.referral.service;

import com.oyuki.referral.entity.Referral;
import com.oyuki.referral.enums.ReferralStatus;
import com.oyuki.referral.repository.ReferralRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.Role;
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

    @Value("${app.referral.normal-reward:200}")
    private BigDecimal normalReward;

    @Value("${app.referral.marketer-reward:2000}")
    private BigDecimal marketerReward;

    @Value("${app.referral.referred-user-reward:0}")
    private BigDecimal referredUserReward;

    @Value("${app.referral.minimum-withdrawal-referrals:20}")
    private int minimumWithdrawalReferrals;

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

        User referrer = referral.getReferrer();
        boolean marketer = referrer.getRole() == Role.MARKETER;
        boolean eligibleBusinessReferral = referredUser.getRole() == Role.SELLER
                || referredUser.getRole() == Role.KITCHEN;

        referral.setStatus(ReferralStatus.QUALIFIED);
        referral.setQualifiedAt(LocalDateTime.now());

        // Marketers earn only for verified Seller/Farmer or Kitchen accounts.
        if (marketer && !eligibleBusinessReferral) {
            referral.setReferrerReward(BigDecimal.ZERO);
            referral.setReferredUserReward(BigDecimal.ZERO);
            referralRepository.save(referral);
            return;
        }

        BigDecimal reward = marketer ? marketerReward : normalReward;

        credit(referrer, reward,
                marketer
                        ? "Marketer reward for verified " + referredUser.getRole() + " registration"
                        : "Referral reward for " + referredUser.getFullName(),
                "REFERRAL-" + referral.getId() + "-REFERRER",
                WalletTransactionType.REFERRAL_REWARD);

        if (referredUserReward.signum() > 0) {
            credit(referredUser, referredUserReward,
                    "Welcome referral reward",
                    "REFERRAL-" + referral.getId() + "-REFERRED",
                    WalletTransactionType.WELCOME_REWARD);
        }

        referral.setReferrerReward(reward);
        referral.setReferredUserReward(referredUserReward);
        referral.setStatus(ReferralStatus.REWARDED);
        referral.setRewardedAt(LocalDateTime.now());
        referralRepository.save(referral);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyReferralSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Referral> referrals = referralRepository.findAllByReferrer_IdOrderByCreatedAtDesc(userId);
        boolean marketer = user.getRole() == Role.MARKETER;

        long verified = referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.QUALIFIED || r.getStatus() == ReferralStatus.REWARDED)
                .count();
        long qualified = qualifiedWithdrawalReferralCount(user, referrals);

        BigDecimal totalEarned = referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.REWARDED)
                .map(Referral::getReferrerReward)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", user.getReferralCode());
        result.put("referralCode", user.getReferralCode());
        result.put("referrerType", marketer ? "MARKETER" : "NORMAL");
        result.put("role", user.getRole());
        result.put("totalInvited", referrals.size());
        result.put("verifiedInvited", verified);
        result.put("qualifiedReferrals", qualified);
        result.put("minimumWithdrawalReferrals", minimumWithdrawalReferrals);
        result.put("remainingForWithdrawal", Math.max(0, minimumWithdrawalReferrals - qualified));
        result.put("withdrawalEligible", qualified >= minimumWithdrawalReferrals);
        result.put("totalEarned", totalEarned);
        result.put("rewardPerVerifiedReferral", marketer ? marketerReward : normalReward);
        result.put("eligibleRoles", marketer ? List.of(Role.SELLER, Role.KITCHEN) : List.of("ANY_VERIFIED_USER"));
        result.put("history", referrals.stream().map(this::mapReferral).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public long getQualifiedWithdrawalReferralCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return qualifiedWithdrawalReferralCount(
                user,
                referralRepository.findAllByReferrer_IdOrderByCreatedAtDesc(userId)
        );
    }

    @Transactional(readOnly = true)
    public void assertReferralWithdrawalEligible(Long userId) {
        long rewardedTransactions = transactionRepository
                .findTop50ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .filter(t -> t.getType() == WalletTransactionType.REFERRAL_REWARD)
                .count();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // A marketer account is always subject to the marketer threshold.
        // Normal accounts are subject to it once they have referral earnings.
        if (user.getRole() != Role.MARKETER && rewardedTransactions == 0) return;

        long qualified = getQualifiedWithdrawalReferralCount(userId);
        if (qualified < minimumWithdrawalReferrals) {
            long remaining = minimumWithdrawalReferrals - qualified;
            String target = user.getRole() == Role.MARKETER
                    ? "verified Seller/Farmer or Kitchen"
                    : "verified";
            throw new IllegalStateException(
                    "You need " + remaining + " more " + target
                            + " referral" + (remaining == 1 ? "" : "s")
                            + " before you can withdraw referral earnings"
            );
        }
    }

    @Transactional
    public void backfillMissingReferralCodes() {
        for (User user : userRepository.findAll()) {
            if (user.getReferralCode() == null || user.getReferralCode().isBlank()) {
                ensureReferralCode(user);
            }
        }
    }

    private long qualifiedWithdrawalReferralCount(User referrer, List<Referral> referrals) {
        return referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.REWARDED)
                .filter(r -> referrer.getRole() != Role.MARKETER
                        || r.getReferredUser().getRole() == Role.SELLER
                        || r.getReferredUser().getRole() == Role.KITCHEN)
                .count();
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
        item.put("referredRole", referral.getReferredUser().getRole());
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
