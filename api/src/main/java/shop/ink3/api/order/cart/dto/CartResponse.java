package shop.ink3.api.order.cart.dto;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.order.cart.entity.Cart;

public record CartResponse(
    Long id,
    Long userId,
    Long bookId,
    String bookTitle,
    int originalBookPrice,
    int saleBookPrice,
    int bookDiscountRate,
    String thumbnailUrl,

    int quantity
) {
    public static CartResponse from(Cart cart) {
        Book book = cart.getBook();
        Long userId = cart.getUser() != null ? cart.getUser().getId() : null;
        return new CartResponse(
            cart.getId(),
            userId,
            book.getId(),
            book.getTitle(),
            book.getOriginalPrice(),
            book.getSalePrice(),
            book.getDiscountRate(),
            book.getThumbnailUrl(),
            cart.getQuantity()
        );
    }

    public static CartResponse from(Cart cart, String presignedUrl) {
        Book book = cart.getBook();
        Long userId = cart.getUser() != null ? cart.getUser().getId() : null;
        return new CartResponse(
            cart.getId(),
            userId,
            book.getId(),
            book.getTitle(),
            book.getOriginalPrice(),
            book.getSalePrice(),
            book.getDiscountRate(),
            presignedUrl,
            cart.getQuantity()
        );
    }
}
