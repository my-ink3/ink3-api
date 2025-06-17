package shop.ink3.api.order.guest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.order.guest.dto.*;
import shop.ink3.api.order.guest.exception.GuestOrderNotFoundException;
import shop.ink3.api.order.guest.service.GuestOrderMainService;
import shop.ink3.api.order.guest.service.GuestOrderService;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.orderBook.dto.OrderBookCreateRequest;
import shop.ink3.api.order.shipment.dto.ShipmentCreateRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import shop.ink3.api.payment.entity.PaymentType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GuestOrderController.class)
class GuestOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GuestOrderService guestOrderService;

    @MockitoBean
    private GuestOrderMainService guestOrderMainService;

    @Test
    @DisplayName("비회원 주문 생성 성공")
    void createGuestOrder_success() throws Exception {
        GuestOrderFormCreateRequest request = new GuestOrderFormCreateRequest(
                new GuestCreateRequest("guest@email.com"),
                new GuestOrderCreateRequest("홍길동", "010-1234-5678"),
                new ShipmentCreateRequest(LocalDate.now(), "수령인", "010-1111-2222", 12345, "서울시 강남구", "상세주소", "상세주소2", 3000, "SHIP123"),
                List.of(new OrderBookCreateRequest(1L, 1L, null, 10000, 1)),
                11000,
                PaymentType.TOSS
        );

        GuestOrderResponse response = new GuestOrderResponse(1L, "UUID123", OrderStatus.CREATED, LocalDateTime.now(), "홍길동", "010-1234-5678");

        when(guestOrderMainService.createGuestOrderForm(any())).thenReturn(response);

        mockMvc.perform(post("/guest-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(response.orderId()));
    }

    @Test
    @DisplayName("비회원 주문 상세 조회 성공")
    void getGuestOrderDetails_success() throws Exception {
        long orderId = 1L;
        GuestOrderDetailsResponse response = new GuestOrderDetailsResponse() {
            public Long getOrderId() { return orderId; }
            public String getOrderUUId() { return "UUID123"; }
            public OrderStatus getStatus() { return OrderStatus.CREATED; }
            public LocalDateTime getOrderedAt() { return LocalDateTime.now(); }
            public String getOrdererName() { return "홍길동"; }
            public String getOrdererPhone() { return "010-1234-5678"; }
            public LocalDate getPreferredDeliveryDate() { return LocalDate.now(); }
            public LocalDateTime getDeliveredAt() { return null; }
            public String getRecipientName() { return "수령인"; }
            public String getRecipientPhone() { return "010-1111-2222"; }
            public Integer getPostalCode() { return 12345; }
            public String getDefaultAddress() { return "서울시 강남구"; }
            public String getExtraAddress() { return "상세주소"; }
            public Integer getShippingFee() { return 3000; }
            public String getShippingCode() { return "SHIP123"; }
            public Integer getPaymentAmount() { return 11000; }
            public PaymentType getPaymentType() { return PaymentType.TOSS; }
            public LocalDateTime getRequestedAt() { return LocalDateTime.now(); }
            public LocalDateTime getApprovedAt() { return LocalDateTime.now(); }
        };

        when(guestOrderService.getGuestOrderDetails(orderId)).thenReturn(response);

        mockMvc.perform(get("/guest-order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId));
    }

    @Test
    @DisplayName("비회원 주문 상세 조회 실패 - 존재하지 않음")
    void getGuestOrderDetails_notFound() throws Exception {
        long orderId = 1L;

        when(guestOrderService.getGuestOrderDetails(orderId)).thenThrow(new GuestOrderNotFoundException());

        mockMvc.perform(get("/guest-order/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }
}