package com.oyuki.farmersday.enums;

public enum FarmersDayStatus {

    /*
     * Admin is still preparing the event.
     */
    DRAFT,

    /*
     * Event has been announced to customers.
     */
    PUBLISHED,

    /*
     * Farmers' Day is currently taking place.
     */
    ACTIVE,

    /*
     * Event has ended.
     */
    COMPLETED,

    /*
     * Event was cancelled.
     */
    CANCELLED
}