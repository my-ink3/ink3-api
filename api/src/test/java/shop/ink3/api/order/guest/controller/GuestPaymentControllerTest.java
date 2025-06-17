package shop.ink3.api.order.guest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import shop.ink3.api.order.guest.dto.GuestPaymentConfirmRequest;
import shop.ink3.api.payment.dto.PaymentResponse;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.service.PaymentService;

@WebMvcTest(GuestPaymentController.class)
class GuestPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private ObjectMapper objectMapper;
    private GuestPaymentConfirmRequest confirmRequest;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        confirmRequest = GuestPaymentConfirmRequest.builder()
            .orderId(42L)
            .paymentKey("pay-key-123456")
            .orderUUID("uuid-1234-abcd")
            .amount(10000)
            .paymentType(PaymentType.TOSS)
            .build();

        paymentResponse = new PaymentResponse(
            1L,
            42L,
            "pay-key-123456",
            1000,
            2000,
            7000,
            PaymentType.TOSS,
            LocalDateTime.of(2025, 6, 17, 15, 0),
            LocalDateTime.of(2025, 6, 17, 15, 1)
        );
    }

    @Test
    @DisplayName("게스트 결제 승인 성공")
    void confirmGuestPaymentSuccess() throws Exception {
        when(paymentService.callPaymentAPI(any())).thenReturn("mock-approve-response");
        when(paymentService.createPayment(any(), eq("mock-approve-response"))).thenReturn(paymentResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/guest-payment/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderId").value(42))
            .andExpect(jsonPath("$.data.paymentAmount").value(7000));
    }

    @Test
    @DisplayName("게스트 결제 실패 처리")
    void failGuestPayment() throws Exception {
        long orderId = 42L;

        mockMvc.perform(MockMvcRequestBuilders.post("/guest-payment/{orderId}/fail", orderId))
            .andExpect(status().isNoContent());

        verify(paymentService).failPayment(eq(orderId), isNull());
    }
}
