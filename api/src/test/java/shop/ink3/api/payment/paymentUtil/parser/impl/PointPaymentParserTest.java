package shop.ink3.api.payment.paymentUtil.parser.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.ink3.api.order.guest.dto.GuestPaymentConfirmRequest;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.payment.dto.PaymentConfirmRequest;
import shop.ink3.api.payment.entity.Payment;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.exception.PaymentParserFailException;
import shop.ink3.api.payment.exception.PointPaymentForGuestException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointPaymentParserTest {

    private PointPaymentParser pointPaymentParser;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        pointPaymentParser = new PointPaymentParser(orderRepository);
    }

    @Test
    @DisplayName("회원 Point 결제 파싱 성공")
    void parseMemberPointPayment_success() {
        long orderId = 1L;
        Order order = Order.builder().id(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentConfirmRequest request = new PaymentConfirmRequest(1L, orderId, null, "UUID123", 1000, 2000, 15000, PaymentType.POINT);

        Payment payment = pointPaymentParser.paymentResponseParser(request, "{}");

        assertThat(payment.getOrder()).isEqualTo(order);
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.POINT);
        assertThat(payment.getPaymentKey()).isNull();
        assertThat(payment.getRequestAt()).isNotNull();
        assertThat(payment.getApprovedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원 Point 결제 파싱 실패 - 주문 없음")
    void parseMemberPointPayment_fail_orderNotFound() {
        long orderId = 99L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        PaymentConfirmRequest request = new PaymentConfirmRequest(1L, orderId, null, "UUID123", 0, 0, 0, PaymentType.POINT);

        assertThatThrownBy(() -> pointPaymentParser.paymentResponseParser(request, "{}"))
                .isInstanceOf(PaymentParserFailException.class)
                .hasMessageContaining("POINT");
    }

    @Test
    @DisplayName("비회원 Point 결제 파싱 실패")
    void parseGuestPointPayment_fail() {
        GuestPaymentConfirmRequest request = GuestPaymentConfirmRequest.builder()
                .orderId(10L)
                .paymentKey(null)
                .orderUUID("UUID-GUEST")
                .amount(0)
                .paymentType(PaymentType.POINT)
                .build();

        assertThatThrownBy(() -> pointPaymentParser.paymentResponseParser(request, "{}"))
                .isInstanceOf(PointPaymentForGuestException.class);
    }
}
