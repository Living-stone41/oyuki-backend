package com.oyuki.order.enums;

public enum PaymentStatus {

    /*
     * Customer has not submitted payment.
     */
    PENDING,

    /*
     * Customer uploaded a transfer receipt.
     */
    AWAITING_CONFIRMATION,

    /*
     * Admin confirmed that Oyuki received the money.
     */
    PAID,

    /*
     * Admin rejected the submitted payment receipt.
     */
    REJECTED,

    FAILED,

    PARTIALLY_REFUNDED,

    REFUNDED
}