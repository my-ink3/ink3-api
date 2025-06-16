package shop.ink3.api.coupon.coupon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import shop.ink3.api.coupon.coupon.entity.Coupon;

public record CouponResponse(
        Long couponId,
        String name,
        Long policyId,
        String policyName,
        int discountRate,
        int discountValue,
        LocalDateTime issuableFrom,
        LocalDateTime expiresAt,
        @JsonProperty("isActive")boolean isActive,
        LocalDateTime createdAt,
        List<BookInfo> books,
        List<CategoryInfo> categories
) {
    public static CouponResponse from(Coupon coupon,
                                      List<BookInfo> books,
                                      List<CategoryInfo> categories) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getCouponPolicy().getId(),
                coupon.getCouponPolicy().getName(),
                coupon.getCouponPolicy().getDiscountPercentage(),
                coupon.getCouponPolicy().getDiscountValue(),
                coupon.getIssuableFrom(),
                coupon.getExpiresAt(),
                coupon.isActive(),
                coupon.getCreatedAt(),
                books == null ? List.of() : books,
                categories == null ? List.of() : categories
        );
    }

    public record BookInfo(Long originId, Long id, String title, String originType) {}
    public record CategoryInfo(Long originId, Long id, String name, String originType) {}
}
