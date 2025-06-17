package shop.ink3.api.payment.paymentUtil.processor.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.ink3.api.payment.dto.PaymentCancelRequest;
import shop.ink3.api.payment.dto.PaymentConfirmRequest;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.exception.PaymentProcessorFailException;
import shop.ink3.api.payment.paymentUtil.client.PaymentClient;
import shop.ink3.api.order.guest.dto.GuestPaymentConfirmRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TossPaymentProcessorTest {

    @Mock
    PaymentClient paymentClient;

    @InjectMocks
    TossPaymentProcessor tossPaymentProcessor;

    @Test
    @DisplayName("Toss 결제 확인 성공 - 회원")
    void processPayment_confirm_success() {
        String expectedResult = "success";
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                1L, 1L, "payKey123", "orderUUID123", 0, 0, 1000, PaymentType.TOSS);

        when(paymentClient.confirmPayment(anyString(), anyMap())).thenReturn(expectedResult);

        String result = tossPaymentProcessor.processPayment(request);

        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("Toss 결제 확인 성공 - 비회원")
    void processPayment_guest_success() {
        String expectedResult = "success";
        GuestPaymentConfirmRequest request = GuestPaymentConfirmRequest.builder()
                .orderId(1L)
                .orderUUID("orderUUID123")
                .paymentKey("payKey123")
                .amount(1000)
                .paymentType(PaymentType.TOSS)
                .build();

        when(paymentClient.confirmPayment(anyString(), anyMap())).thenReturn(expectedResult);

        String result = tossPaymentProcessor.processPayment(request);

        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("Toss 결제 확인 실패")
    void processPayment_confirm_fail() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                1L, 1L, "payKey123", "orderUUID123", 0, 0, 1000, PaymentType.TOSS);

        when(paymentClient.confirmPayment(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Toss API error"));

        assertThrows(PaymentProcessorFailException.class, () -> {
            tossPaymentProcessor.processPayment(request);
        });
    }

    @Test
    @DisplayName("Toss 결제 취소 성공")
    void cancelPayment_success() {
        String expectedResult = "cancelled";
        PaymentCancelRequest cancelRequest = new PaymentCancelRequest(
                1L, "payKey123", 1000, PaymentType.TOSS, "고객 요청");

        when(paymentClient.cancelPayment(anyString(), eq("payKey123"), anyMap())).thenReturn(expectedResult);

        String result = tossPaymentProcessor.cancelPayment(cancelRequest);

        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("Toss 결제 취소 실패")
    void cancelPayment_fail() {
        PaymentCancelRequest cancelRequest = new PaymentCancelRequest(
                1L, "payKey123", 1000, PaymentType.TOSS, "고객 요청");

        when(paymentClient.cancelPayment(anyString(), eq("payKey123"), anyMap()))
                .thenThrow(new RuntimeException("취소 실패"));

        assertThrows(PaymentProcessorFailException.class, () -> {
            tossPaymentProcessor.cancelPayment(cancelRequest);
        });
    }
}
