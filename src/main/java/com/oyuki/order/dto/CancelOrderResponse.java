package com.oyuki.order.dto;

import com.oyuki.refund.dto.RefundResponse;

public record CancelOrderResponse(

        String message,

        boolean refundCreated,

        AdminOrderResponse order,

        RefundResponse refund

) {
}