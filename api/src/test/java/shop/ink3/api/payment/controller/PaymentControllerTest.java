package shop.ink3.api.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.payment.dto.*;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.service.PaymentService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private final long orderId = 1L;
    private final long userId = 100L;

    @Test
    @DisplayName("결제 승인 요청 - 성공")
    void confirmPayment_success() throws Exception {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                orderId, userId, "paymentKey-123", "orderUUID-1",
                0, 0, 10000, PaymentType.TOSS
        );
        String approveResponse = "mock-approve-json";
        PaymentResponse response = new PaymentResponse(
                1L,
                orderId,
                "abcde",
                0,
                0,
                10000,
                PaymentType.TOSS
                , LocalDateTime.now(),
                LocalDateTime.now());
        Mockito.when(paymentService.callPaymentAPI(any())).thenReturn(approveResponse);
        Mockito.when(paymentService.createPayment(eq(request), eq(approveResponse))).thenReturn(response);

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.paymentAmount").value(10000));
    }

    @Test
    @DisplayName("결제 승인 요청 - 실패")
    void confirmPayment_실패() throws Exception {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                orderId, userId, null, "orderUUID-1",
                0, 0, 10000, PaymentType.TOSS
        );
        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("결제 실패 요청 - 성공")
    void failPayment_success() throws Exception {
        mockMvc.perform(post("/payments/{orderId}/fail", orderId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(paymentService).failPayment(orderId, userId);
    }

    @Test
    @DisplayName("결제 취소 요청 - 성공")
    void cancelPayment_success() throws Exception {
        PaymentCancelRequest cancelRequest = new PaymentCancelRequest(orderId, "abcde", 10000, PaymentType.TOSS, "변심");

        mockMvc.perform(post("/payments/{orderId}/cancel", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isNoContent());

        verify(paymentService).cancelPayment(orderId, userId, cancelRequest);
    }

    @Test
    @DisplayName("결제 조회 - 성공")
    void getPayment_success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L,
                orderId,
                "abcde",
                0,
                0,
                10000,
                PaymentType.TOSS
                , LocalDateTime.now(),
                LocalDateTime.now());
        Mockito.when(paymentService.getPayment(orderId)).thenReturn(response);

        mockMvc.perform(get("/payments/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId));
    }

    @Test
    @DisplayName("결제 삭제 - 성공")
    void deletePayment_success() throws Exception {
        mockMvc.perform(delete("/payments/{orderId}", orderId))
                .andExpect(status().isNoContent());

        verify(paymentService).deletePayment(orderId);
    }
}
