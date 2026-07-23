package com.oyuki.tracking.enums;

public enum OrderDeliveryStatus {

    /*
     * Admin assigned a rider to the order.
     */
    ASSIGNED,

    /*
     * Rider accepted the delivery job.
     */
    ACCEPTED,

    /*
     * Rider collected the complete order
     * from the Oyuki admin hub.
     */
    PICKED_UP,

    /*
     * Rider is travelling to the customer.
     */
    OUT_FOR_DELIVERY,

    /*
     * Customer received the order.
     */
    DELIVERED,

    /*
     * Admin or rider cancelled the delivery.
     */
    CANCELLED,

    /*
     * Delivery could not be completed.
     */
    FAILED
}