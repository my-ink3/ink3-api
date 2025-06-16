package shop.ink3.api.order.cart.dto;

import java.util.List;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.coupon.store.dto.CouponStoreDto;
import shop.ink3.api.order.cart.entity.Cart;

public record CartCouponResponse(
    Long id,
    Long userId,
    Long bookId,
    String bookTitle,
    int originalBookPrice,
    int saleBookPrice,
    int bookDiscountRate,
    String thumbnailUrl,
    boolean isPackable,
    int quantity,
    List<CouponStoreDto> applicableCoupons
) {
    public static CartCouponResponse from(Cart cart, List<CouponStoreDto> coupons, String presignedUrl) {
        Book book = cart.getBook();
        Long userId = cart.getUser() != null ? cart.getUser().getId() : null;
        return new CartCouponResponse(
            cart.getId(),
            userId,
            book.getId(),
            book.getTitle(),
            book.getOriginalPrice(),
            book.getSalePrice(),
            book.getDiscountRate(),
            presignedUrl,
            book.isPackable(),
            cart.getQuantity(),
            coupons
        );
    }
}
