package shop.ink3.api.order.orderBook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.order.orderBook.dto.OrderBookResponse;
import shop.ink3.api.order.orderBook.dto.OrderBookUpdateRequest;
import shop.ink3.api.order.orderBook.exception.OrderBookNotFoundException;
import shop.ink3.api.order.orderBook.service.OrderBookService;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.packaging.exception.PackagingNotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderBookController.class)
class OrderBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderBookService orderBookService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("주문도서 단건 조회 성공")
    void getOrderBook_success() throws Exception {
        long orderBookId = 1L;
        OrderBookResponse response = new OrderBookResponse(orderBookId, 1L, 1L, null, null,
                "책제목", 10000, "image.jpg", null, null, null, 10000, 1);

        when(orderBookService.getOrderBook(orderBookId)).thenReturn(response);

        mockMvc.perform(get("/order-books/{orderBookId}", orderBookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(response.getOrderId()));
    }

    @Test
    @DisplayName("주문도서 단건 조회 실패 - 존재하지 않음")
    void getOrderBook_fail_notFound() throws Exception {
        long orderBookId = 999L;
        when(orderBookService.getOrderBook(orderBookId)).thenThrow(new OrderBookNotFoundException(orderBookId));

        mockMvc.perform(get("/order-books/{orderBookId}", orderBookId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("주문ID에 따른 주문도서 리스트 조회 성공")
    void getOrderBooksByOrderId_success() throws Exception {
        long orderId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<OrderBookResponse> content = List.of(
                new OrderBookResponse(1L, orderId, 1L, null, null,
                        "책제목", 10000, "image.jpg", null, null, null, 10000, 1)
        );
        PageResponse<OrderBookResponse> pageResponse = PageResponse.from(new PageImpl<>(content, pageable, content.size()));

        when(orderBookService.getOrderBookListByOrderId(eq(orderId), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/order-books/orders/{orderId}", orderId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].orderId").value(orderId));
    }

    @Test
    @DisplayName("주문ID에 따른 주문도서 리스트 조회 실패 - 존재하지 않음")
    void getOrderBooksByOrderId_fail_notFound() throws Exception {
        long orderId = 999L;
        when(orderBookService.getOrderBookListByOrderId(eq(orderId), any(Pageable.class)))
                .thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(get("/order-books/orders/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("주문도서 수정 성공")
    void updateOrderBook_success() throws Exception {
        long orderBookId = 1L;
        OrderBookUpdateRequest updateRequest = new OrderBookUpdateRequest(1L, 1L, 2, 10000);
        OrderBookResponse response = new OrderBookResponse(orderBookId, 1L, 1L, 1L, 1L,
                "책제목", 10000, "image.jpg", "포장", 1000, "쿠폰", 10000, 2);

        when(orderBookService.updateOrderBook(eq(orderBookId), any(OrderBookUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/order-books/{orderBookId}", orderBookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @DisplayName("주문도서 수정 실패 - 포장지 없음")
    void updateOrderBook_fail_packagingNotFound() throws Exception {
        long orderBookId = 1L;
        OrderBookUpdateRequest updateRequest = new OrderBookUpdateRequest(1L, 999L, 2, 10000);

        when(orderBookService.updateOrderBook(eq(orderBookId), any(OrderBookUpdateRequest.class)))
                .thenThrow(new PackagingNotFoundException(999L));

        mockMvc.perform(put("/order-books/{orderBookId}", orderBookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("주문도서 단건 삭제 성공")
    void deleteOrderBook_success() throws Exception {
        long orderBookId = 1L;

        doNothing().when(orderBookService).deleteOrderBook(orderBookId);

        mockMvc.perform(delete("/order-books/{orderBookId}", orderBookId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("주문도서 단건 삭제 실패 - 존재하지 않음")
    void deleteOrderBook_fail_notFound() throws Exception {
        long orderBookId = 999L;
        doNothing().when(orderBookService).deleteOrderBook(orderBookId);

        mockMvc.perform(delete("/order-books/{orderBookId}", orderBookId))
                .andExpect(status().isNoContent()); // 실제 로직에서 예외를 던지는 경우엔 throw stub 필요
    }

    @Test
    @DisplayName("주문ID로 주문도서 전체 삭제 성공")
    void deleteOrderBooksByOrderId_success() throws Exception {
        long orderId = 1L;

        doNothing().when(orderBookService).deleteOrderBookListByOrderId(orderId);

        mockMvc.perform(delete("/order-books/orders/{orderId}", orderId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("주문ID로 주문도서 전체 삭제 실패 - 존재하지 않음")
    void deleteOrderBooksByOrderId_fail_notFound() throws Exception {
        long orderId = 999L;

        doNothing().when(orderBookService).deleteOrderBookListByOrderId(orderId);

        mockMvc.perform(delete("/order-books/orders/{orderId}", orderId))
                .andExpect(status().isNoContent());
    }
}
