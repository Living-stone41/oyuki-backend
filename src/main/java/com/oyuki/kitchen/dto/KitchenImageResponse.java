package com.oyuki.kitchen.dto;

import com.oyuki.kitchen.entity.KitchenImage;

public record KitchenImageResponse(Long id, String imageUrl, String caption, Integer displayOrder) {
    public static KitchenImageResponse from(KitchenImage image) {
        return new KitchenImageResponse(image.getId(), image.getImageUrl(), image.getCaption(), image.getDisplayOrder());
    }
}
