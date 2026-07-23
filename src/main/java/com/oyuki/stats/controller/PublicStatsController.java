package com.oyuki.stats.controller;

import com.oyuki.order.enums.OrderStatus;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.review.entity.Review;
import com.oyuki.review.enums.ReviewStatus;
import com.oyuki.review.repository.ReviewRepository;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/public/stats")
public class PublicStatsController {
    private final UserRepository users;
    private final OrderRepository orders;
    private final ReviewRepository reviews;

    public PublicStatsController(UserRepository users, OrderRepository orders, ReviewRepository reviews) {
        this.users = users; this.orders = orders; this.reviews = reviews;
    }

    @GetMapping
    public Map<String,Object> stats() {
        List<Review> approved = reviews.findAllByStatusOrderByCreatedAtDesc(ReviewStatus.VISIBLE);
        double rating = approved.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return Map.of(
                "farmers", users.countByRole(Role.SELLER),
                "kitchens", users.countByRole(Role.KITCHEN),
                "orders", orders.countByStatus(OrderStatus.DELIVERED),
                "rating", Math.round(rating * 10.0) / 10.0,
                "ratings", approved.size()
        );
    }
}
