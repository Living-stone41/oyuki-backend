package com.oyuki.kitchen.repository;

import com.oyuki.kitchen.entity.KitchenImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KitchenImageRepository extends JpaRepository<KitchenImage, Long> {
    List<KitchenImage> findAllByKitchenProfileIdOrderByDisplayOrderAscIdAsc(Long kitchenProfileId);
    long countByKitchenProfileId(Long kitchenProfileId);
}
