package shop.ink3.api.coupon.rabbitMq.consume;

import java.time.LocalDateTime;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import shop.ink3.api.coupon.coupon.dto.CouponCreateRequest;
import shop.ink3.api.coupon.coupon.dto.CouponResponse;
import shop.ink3.api.coupon.coupon.service.Impl.CouponServiceImpl;
import shop.ink3.api.coupon.rabbitMq.message.WelcomeCouponMessage;
import shop.ink3.api.coupon.store.dto.CommonCouponIssueRequest;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.coupon.store.service.CouponStoreService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeCouponConsumer {
    private final CouponStoreService couponStoreService;
    private final CouponServiceImpl couponService;

    @RabbitListener(queues = "coupon.welcome", containerFactory = "pojoListenerContainerFactory")
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void consumeWelcome(WelcomeCouponMessage message) {
        try{
            log.info("수신 성공!");
            System.out.println(message.userId());
            CouponCreateRequest couponCreateRequest = new CouponCreateRequest(1L, "WELCOME", LocalDateTime.now(), LocalDateTime.now().plusDays(30), true, Collections.emptyList(), Collections.emptyList());

            CouponResponse coupon = couponService.createCoupon(couponCreateRequest);
            Long couponId = coupon.couponId();
            log.info("userId={}, couponId={}", message.userId(), couponId);
            couponStoreService.issueCommonCoupon(
                    new CommonCouponIssueRequest(message.userId(), couponId, OriginType.WELCOME, null)
            );
            log.info("쿠폰 발급 성공~!");
        }catch (Exception e){
            log.error("❌ 쿠폰 발급 실패 - payload: {}", message, e);
            throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
        }
    }

    @RabbitListener(
            queues = "coupon.welcome.dead",
            containerFactory = "pojoListenerContainerFactory"
    )
    public void consumeFailedMessage(WelcomeCouponMessage message) {
        log.error("💀 DLQ에 빠진 WelcomeCouponMessage 처리: {}", message);
        // TODO: DB 기록, 수동 재처리 로직 등
    }
}



