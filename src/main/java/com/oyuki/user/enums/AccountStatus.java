package com.oyuki.user.enums;

public enum AccountStatus {
      // User has registered but has not verified their email or phone
    PENDING_VERIFICATION,

    // Seller or kitchen has verified their contact but needs admin approval
    PENDING_APPROVAL,

    // Account can use the platform
    ACTIVE,

    // Admin rejected the seller or kitchen application
    REJECTED,

    // Account has been temporarily blocked
    SUSPENDED,

    // Account has been disabled
    DISABLED
}

