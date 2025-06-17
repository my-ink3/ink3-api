package shop.ink3.api.order.order.exception;

public class InsufficientBookStockException extends RuntimeException{
    public InsufficientBookStockException(String bookTitle, int requestedQty, int availableQty) {
        super(String.format("도서 %s의 재고가 부족합니다. %n 요청량=%d, 현재고=%d", bookTitle, requestedQty, availableQty));
    }
}
