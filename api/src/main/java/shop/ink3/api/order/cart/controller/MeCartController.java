package shop.ink3.api.order.cart.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.order.cart.dto.CartCouponResponse;
import shop.ink3.api.order.cart.dto.CartRequest;
import shop.ink3.api.order.cart.dto.CartResponse;
import shop.ink3.api.order.cart.dto.CartUpdateRequest;
import shop.ink3.api.order.cart.dto.MeCartRequest;
import shop.ink3.api.order.cart.service.CartService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/me/carts")
public class MeCartController {
    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CommonResponse<CartResponse>> addCart(@RequestHeader(name = "X-User-Id") Long userId, @RequestBody @Valid MeCartRequest request) {
        CartRequest cartRequest = new CartRequest(userId, request.bookId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(cartService.addCartItem(cartRequest)));
    }

    @PostMapping("/merge-guest")
    public ResponseEntity<Void> mergeGuestCart(@RequestBody List<MeCartRequest> guestItems,
        @RequestHeader("X-User-Id") Long userId) {
        for (MeCartRequest item : guestItems) {
            CartRequest request = new CartRequest(userId, item.bookId(), item.quantity());
            cartService.addCartItem(request);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cartId}")
    public ResponseEntity<CommonResponse<CartResponse>> updateQuantity(@RequestHeader(name = "X-User-Id") Long userId, @PathVariable Long cartId, @RequestBody @Valid CartUpdateRequest request) {
        return ResponseEntity.ok(CommonResponse.update(cartService.updateCartQuantity(cartId, request)));
    }

    @GetMapping("/coupons")
    public ResponseEntity<CommonResponse<List<CartCouponResponse>>> getCartsWithCoupon(@RequestHeader(name = "X-User-Id") Long userId) {
        List<CartCouponResponse> carts = cartService.getCartItemsWithCoupons(userId);
        log.warn(carts.toString());
        return ResponseEntity.ok(CommonResponse.success(carts));
    }

    @GetMapping("/selected/coupons")
    public ResponseEntity<CommonResponse<List<CartCouponResponse>>> getSelectCartsWithCoupon(@RequestHeader(name = "X-User-Id") Long userId, @RequestParam(required = false) List<Long> cartIds) {
        List<CartCouponResponse> carts = cartService.getSelectCartsWithCoupon(userId, cartIds);
        log.warn(carts.toString());
        return ResponseEntity.ok(CommonResponse.success(carts));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<CartResponse>>> getCarts(@RequestHeader(name = "X-User-Id") Long userId) {
        List<CartResponse> carts = cartService.getCartItems(userId);
        log.warn(carts.toString());
        return ResponseEntity.ok(CommonResponse.success(carts));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCarts(@RequestHeader(name = "X-User-Id") Long userId) {
        cartService.deleteCartItems(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteCart(@RequestHeader(name = "X-User-Id") Long userId, @PathVariable Long cartId) {
        cartService.deleteCartItem(cartId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
