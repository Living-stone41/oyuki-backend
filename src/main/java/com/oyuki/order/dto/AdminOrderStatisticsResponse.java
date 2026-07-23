package com.oyuki.order.dto;

import java.math.BigDecimal;

public record AdminOrderStatisticsResponse(

        long totalOrders,

        long pendingOrders,
        long confirmedOrders,
        long processingOrders,
        long readyForPickupOrders,
        long outForDeliveryOrders,
        long deliveredOrders,
        long cancelledOrders,
        long rejectedOrders,

        long awaitingPaymentConfirmation,
        long paidOrders,
        long failedPayments,
        long refundedOrders,

        BigDecimal totalOrderValue,
        BigDecimal confirmedRevenue,
        BigDecimal refundedAmount,
        BigDecimal netRevenue

) {
}