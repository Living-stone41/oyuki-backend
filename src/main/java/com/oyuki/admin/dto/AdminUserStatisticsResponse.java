package com.oyuki.admin.dto;

public record AdminUserStatisticsResponse(

        long totalUsers,
        long customers,
        long sellers,
        long kitchens,
        long riders,
        long administrators,

        long pendingVerification,
        long pendingApproval,
        long active,
        long rejected,
        long suspended,
        long disabled

) {
}
