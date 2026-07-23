package com.oyuki.logistics.enums;

public enum DeliveryStatus {

    /*
     * Seller or kitchen has prepared the item,
     * but no rider has been assigned.
     */
    PENDING_ASSIGNMENT,

    /*
     * Logistics admin assigned a rider.
     */
    ASSIGNED,

    /*
     * Rider accepted the delivery request.
     */
    ACCEPTED,

    /*
     * Rider collected the item from the
     * seller or kitchen.
     */
    PICKED_UP,

    /*
     * Rider is moving toward the destination.
     */
    OUT_FOR_DELIVERY,

    /*
     * Customer or destination kitchen received it.
     */
    DELIVERED,

    /*
     * Delivery was cancelled.
     */
    CANCELLED,

    /*
     * Delivery could not be completed.
     */
    FAILED
}