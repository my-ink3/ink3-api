package shop.ink3.api.payment.paymentUtil.processor.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.ink3.api.order.guest.dto.GuestPaymentConfirmRequest;
import shop.ink3.api.payment.dto.PaymentCancelRequest;
import shop.ink3.api.payment.dto.PaymentConfirmRequest;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.exception.PaymentKeyNotExistsException;

import static org.assertj.core.api.Assertions.*;

class PointPaymentProcessorTest {

    private final PointPaymentProcessor processor = new PointPaymentProcessor();

    @Test
    @DisplayName("회원 포인트 결제 성공 - 반환값 null")
    void processPayment_forMember_success() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                1L, 1L, null, "ORDER_UUID", 0, 0, 0, PaymentType.POINT
        );

        String result = processor.processPayment(request);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("비회원 포인트 결제 실패 - 예외 발생")
    void processPayment_forGuest_throwsException() {
        GuestPaymentConfirmRequest request = GuestPaymentConfirmRequest.builder()
                .orderId(1L)
                .paymentKey(null)
                .orderUUID("ORDER_UUID")
                .amount(0)
                .paymentType(PaymentType.POINT)
                .build();

        assertThatThrownBy(() -> processor.processPayment(request))
                .isInstanceOf(PaymentKeyNotExistsException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("포인트 결제 취소 성공 - 반환값 null")
    void cancelPayment_success() {
        PaymentCancelRequest request = new PaymentCancelRequest(
                1L, null, 0, PaymentType.POINT, "사용자 요청"
        );

        String result = processor.cancelPayment(request);
        assertThat(result).isNull();
    }
}