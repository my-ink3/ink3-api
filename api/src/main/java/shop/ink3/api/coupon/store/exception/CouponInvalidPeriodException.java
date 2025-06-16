package shop.ink3.api.coupon.store.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CouponInvalidPeriodException extends RuntimeException {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public CouponInvalidPeriodException() {
        super("Coupon invalid period error!");
    }

    public CouponInvalidPeriodException(String message) {
        super(message);
    }

    public CouponInvalidPeriodException(LocalDateTime issuableFrom, LocalDateTime expiresAt) {
        super(String.format("쿠폰의 사용 가능 기간이 아닙니다.\n사용 가능일: %s ~ %s",
                issuableFrom.format(FORMATTER),
                expiresAt.format(FORMATTER)));
    }
}
