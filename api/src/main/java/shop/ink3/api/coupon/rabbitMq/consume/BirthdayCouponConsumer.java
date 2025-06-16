package shop.ink3.api.coupon.rabbitMq.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import shop.ink3.api.coupon.coupon.dto.CouponCreateRequest;
import shop.ink3.api.coupon.coupon.dto.CouponResponse;
import shop.ink3.api.coupon.coupon.service.Impl.CouponServiceImpl;
import shop.ink3.api.coupon.rabbitMq.message.BirthdayCouponMessage;
import shop.ink3.api.coupon.store.dto.CommonCouponIssueRequest;
import shop.ink3.api.coupon.store.entity.CouponStatus;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.coupon.store.repository.CouponStoreRepository;
import shop.ink3.api.coupon.store.service.CouponStoreService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayCouponConsumer {

    private final ObjectMapper objectMapper;
    private final CouponServiceImpl couponService;
    private final CouponStoreService couponStoreService;
    private final CouponStoreRepository couponStoreRepository;

    @RabbitListener(queues = "coupon.birthday")
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void consumeBulk(String payload) {
        try {
            log.info("📥 수신됨 - raw: {}", payload);

            BirthdayCouponMessage message = objectMapper.readValue(payload, BirthdayCouponMessage.class);
            // 정책 ID 1L은 실제 존재하는 값이어야 함
            CouponCreateRequest couponCreateRequest = new CouponCreateRequest(
                    1L, "BIRTHDAY",
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    true, Collections.emptyList(), Collections.emptyList()
            );
            CouponResponse coupon = couponService.createCoupon(couponCreateRequest);

            message.userIds().forEach(userId -> {
                boolean alreadyIssued = couponStoreRepository.existsByStatusAndUserIdAndOriginType(
                        CouponStatus.READY, userId, OriginType.BIRTHDAY
                );
                if (!alreadyIssued) {
                    couponStoreService.issueCommonCoupon(
                            new CommonCouponIssueRequest(userId, coupon.couponId(), OriginType.BIRTHDAY, null)
                    );
                    log.info("✅ userId={} 발급 성공", userId);
                } else {
                    log.info("⚠️ userId={} 이미 발급되어 생략", userId);
                }
            });

        } catch (Exception e) {
            log.error("❌ 생일 쿠폰 발급 실패 - payload: {}", payload, e);
            throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
        }
    }

    @RabbitListener(queues = "coupon.birthday.dead")
    public void consumeFailedMessage(String payload) {
        try {
            BirthdayCouponMessage message = objectMapper.readValue(payload, BirthdayCouponMessage.class);
            log.error("💀 DLQ에 빠진 메시지 처리: {}", message);
            // TODO: DB 기록, 수동 재처리 로직 등
        } catch (Exception e) {
            log.error("DLQ 메시지 파싱 실패 - payload: {}", payload, e);
            // 필요시 예외 던지거나 별도 알림
        }
    }
}
