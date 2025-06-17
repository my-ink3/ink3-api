package shop.ink3.api.payment.paymentUtil.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shop.ink3.api.order.guest.dto.GuestPaymentConfirmRequest;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.payment.dto.PaymentConfirmRequest;
import shop.ink3.api.payment.dto.TossPaymentResponse;
import shop.ink3.api.payment.entity.Payment;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.exception.PaymentParserFailException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TossPaymentParserTest {

    private TossPaymentParser tossPaymentParser;
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        tossPaymentParser = new TossPaymentParser(orderRepository);
    }

    @Test
    @DisplayName("회원 Toss 결제 응답 파싱 성공")
    void parseMemberPayment_success() throws JsonProcessingException {
        long orderId = 1L;
        Order order = Order.builder().id(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        TossPaymentResponse response = new TossPaymentResponse(
                "pay_key_123",
                15000,
                OffsetDateTime.of(2025, 6, 17, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2025, 6, 17, 10, 5, 0, 0, ZoneOffset.UTC)
        );

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = objectMapper.writeValueAsString(response);

        PaymentConfirmRequest request = new PaymentConfirmRequest(1L, orderId, "pay_key_123", "UUID123", 0, 0, 15000, PaymentType.TOSS);
        Payment payment = tossPaymentParser.paymentResponseParser(request, json);

        assertThat(payment.getPaymentKey()).isEqualTo("pay_key_123");
        assertThat(payment.getPaymentAmount()).isEqualTo(15000);
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.TOSS);
    }

    @Test
    @DisplayName("비회원 Toss 결제 응답 파싱 성공")
    void parseGuestPayment_success() throws JsonProcessingException {
        long orderId = 2L;
        Order order = Order.builder().id(orderId).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        TossPaymentResponse response = new TossPaymentResponse(
                "guest_key_456",
                20000,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(2)
        );

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = objectMapper.writeValueAsString(response);

        GuestPaymentConfirmRequest request = GuestPaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey("guest_key_456")
                .orderUUID("UUID456")
                .amount(20000)
                .paymentType(PaymentType.TOSS)
                .build();

        Payment payment = tossPaymentParser.paymentResponseParser(request, json);

        assertThat(payment.getPaymentKey()).isEqualTo("guest_key_456");
        assertThat(payment.getPaymentAmount()).isEqualTo(20000);
    }

    @Test
    @DisplayName("Toss 응답 파싱 실패 - Order 없음")
    void parsePayment_fail_orderNotFound() throws JsonProcessingException {
        long orderId = 99L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        TossPaymentResponse response = new TossPaymentResponse(
                "fail_key",
                30000,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusMinutes(3)
        );

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = objectMapper.writeValueAsString(response);

        GuestPaymentConfirmRequest request = GuestPaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey("fail_key")
                .orderUUID("UUID789")
                .amount(30000)
                .paymentType(PaymentType.TOSS)
                .build();

        assertThatThrownBy(() -> tossPaymentParser.paymentResponseParser(request, json))
                .isInstanceOf(PaymentParserFailException.class)
                .hasMessageContaining("TOSS");
    }
}
