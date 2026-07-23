package com.oyuki.refund.enums;

public enum RefundStatus {

    /*
     * Refund has been created but not processed.
     */
    PENDING,

    /*
     * Admin has started processing the refund.
     */
    PROCESSING,

    /*
     * Customer has received the money.
     */
    COMPLETED,

    /*
     * Refund attempt failed.
     */
    FAILED,

    /*
     * Refund record was cancelled.
     */
    CANCELLED
}