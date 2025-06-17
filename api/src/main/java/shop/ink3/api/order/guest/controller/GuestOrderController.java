package shop.ink3.api.order.guest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.order.guest.dto.GuestOrderDetailsResponse;
import shop.ink3.api.order.guest.dto.GuestOrderFormCreateRequest;
import shop.ink3.api.order.guest.dto.GuestOrderResponse;
import shop.ink3.api.order.guest.service.GuestOrderMainService;
import shop.ink3.api.order.guest.service.GuestOrderService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/guest-order")
public class GuestOrderController {
    private final GuestOrderService guestOrderService;
    private final GuestOrderMainService guestOrderMainService;

    // 비회원 주문 생성
    @PostMapping
    public ResponseEntity<CommonResponse<GuestOrderResponse>> createGuestOrder(
            @RequestBody GuestOrderFormCreateRequest guestOrderFormCreateRequest){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CommonResponse.create(guestOrderMainService.createGuestOrderForm(guestOrderFormCreateRequest)));
    }

    // 비회원의 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<CommonResponse<GuestOrderDetailsResponse>> getGuestDetails(
            @PathVariable long orderId) {
        GuestOrderDetailsResponse guestOrderDetails = guestOrderService.getGuestOrderDetails(orderId);
        return ResponseEntity.ok(CommonResponse.success(guestOrderDetails));
    }
}
