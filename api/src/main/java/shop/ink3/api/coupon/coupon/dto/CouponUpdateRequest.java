package shop.ink3.api.coupon.coupon.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CouponUpdateRequest(
        @NotNull Long policyId,
        @NotBlank String name,
        @NotNull LocalDateTime issuableFrom,
        @NotNull LocalDateTime expiresAt,
        @JsonProperty("isActive")boolean isActive,
        List<Long> bookIdList,
        List<Long> categoryIdList
) {}

