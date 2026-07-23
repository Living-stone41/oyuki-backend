package com.oyuki.order.controller;

import com.oyuki.order.dto.AdminOrderResponse;
import com.oyuki.order.service.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(
            AdminOrderService adminOrderService
    ) {
        this.adminOrderService =
                adminOrderService;
    }

    /**
     * View every customer order.
     */
    @GetMapping
    public ResponseEntity<List<AdminOrderResponse>>
    getAllOrders(
            Authentication authentication
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                adminOrderService.getAllOrders(
                        adminId
                )
        );
    }

    /**
     * View one complete order.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponse>
    getOrderById(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        Long adminId =
                getUserId(authentication);

        return ResponseEntity.ok(
                adminOrderService.getOrderById(
                        adminId,
                        orderId
                )
        );
    }

    private Long getUserId(
            Authentication authentication
    ) {
        return (Long) authentication.getPrincipal();
    }
    @PatchMapping(
        "/items/{orderItemId}/received"
)
public ResponseEntity<AdminOrderResponse>
markOrderItemReceived(
        Authentication authentication,
        @PathVariable Long orderItemId
) {
    Long adminId =
            getUserId(authentication);

    return ResponseEntity.ok(
            adminOrderService
                    .markOrderItemReceived(
                            adminId,
                            orderItemId
                    )
    );
}
}