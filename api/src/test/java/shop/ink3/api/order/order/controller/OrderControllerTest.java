package shop.ink3.api.order.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.order.order.dto.OrderCreateRequest;
import shop.ink3.api.order.order.dto.OrderDateRequest;
import shop.ink3.api.order.order.dto.OrderFormCreateRequest;
import shop.ink3.api.order.order.dto.OrderResponse;
import shop.ink3.api.order.order.dto.OrderStatusRequest;
import shop.ink3.api.order.order.dto.OrderUpdateRequest;
import shop.ink3.api.order.order.dto.OrderWithDetailsResponse;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.order.service.OrderMainService;
import shop.ink3.api.order.order.service.OrderService;
import shop.ink3.api.order.orderBook.dto.OrderBookCreateRequest;
import shop.ink3.api.order.shipment.dto.ShipmentCreateRequest;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.user.user.entity.User;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {
    @MockitoBean
    OrderService orderService;

    @MockitoBean
    OrderMainService orderMainService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("주문 생성 - 성공")
    void createOrder_성공() throws Exception {
        // given
        OrderCreateRequest orderCreateRequest = new OrderCreateRequest(
                1L,
                "권용민",
                "010-9926-8961"
        );
        ShipmentCreateRequest shipmentCreateRequest = new ShipmentCreateRequest(
                LocalDate.now(),
                "권용민",
                "010-9926-8961",
                101010,
                "서창남순환로82",
                "101동 603호",
                "",
                3000,
                null
        );
        OrderBookCreateRequest orderBookCreateRequest = new OrderBookCreateRequest(
                1L,
                null,
                null,
                30000,
                2
        );
        OrderFormCreateRequest orderFormCreateRequest = new OrderFormCreateRequest(
                orderCreateRequest,
                shipmentCreateRequest,
                List.of(orderBookCreateRequest),
                0,
                0,
                33000,
                PaymentType.TOSS
        );
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).status(OrderStatus.CONFIRMED).build();
        OrderResponse orderResponse = OrderResponse.from(order);
        when(orderMainService.createOrderForm(any())).thenReturn(orderResponse);

        // when, then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderFormCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("주문 조회 - 성공")
    void getOrder_성공() throws Exception {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        OrderResponse orderResponse = OrderResponse.from(order);
        when(orderService.getOrder(anyLong())).thenReturn(orderResponse);

        // when, then
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
        verify(orderService).getOrder(1L);
    }

    @Test
    @DisplayName("주문 조회 - 실패")
    void getOrder_실패() throws Exception {
        // given
        doThrow(new OrderNotFoundException(1L)).when(orderService).getOrder(anyLong());

        // when, then
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
        verify(orderService).getOrder(1L);
    }


    @Test
    @DisplayName("사용자의 주문 리스트 조회 - 성공")
    void getOrderListByUser_성공() throws Exception {
        class MockOrderWithDetailsResponse implements OrderWithDetailsResponse {
            public Long getId() { return 1L; }
            public String getOrderUUID() { return "order-uuid"; }
            public OrderStatus getStatus() { return OrderStatus.CREATED; }
            public LocalDateTime getOrderedAt() { return LocalDateTime.now(); }
            public String getOrdererName() { return "홍길동"; }
            public String getOrdererPhone() { return "010-1234-5678"; }
            public Integer getPaymentAmount() { return 10000; }
            public String getRepresentativeBookName() { return "책 제목"; }
            public String getRepresentativeThumbnailUrl() { return "/img/book.jpg"; }
            public Integer getBookTypeCount() { return 1; }
            public Long getOrderBookId() { return 1L; }
            public Long getBookId() { return 100L; }
            public Long getHasReview() { return 0L; }
        }

        // given
        PageResponse<OrderWithDetailsResponse> response =
                PageResponse.from(new PageImpl<>(List.of(new MockOrderWithDetailsResponse())));
        when(orderService.getOrderListByUser(anyLong(), any(Pageable.class))).thenReturn(response);

        // when, then
        mockMvc.perform(get("/orders/me")
                        .header("X-User-Id", "1")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L));
    }

    @Test
    @DisplayName("사용자의 기간별 주문 리스트 조회 - 성공")
    void getOrderListByUserAndDate_성공() throws Exception {
        // given
        OrderDateRequest dateRequest = new OrderDateRequest(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 31, 23, 59)
        );
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        PageResponse<OrderResponse> pageResponse =
                PageResponse.from(new PageImpl<>(List.of(OrderResponse.from(order))));
        when(orderService.getOrderListByUserAndDate(anyLong(), any(), any())).thenReturn(pageResponse);

        // when, then
        mockMvc.perform(get("/orders/me/date")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dateRequest))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("사용자의 주문 상태별 주문 리스트 조회 - 성공")
    void getOrderListByUserAndStatus_성공() throws Exception {
        // given
        OrderStatusRequest statusRequest = new OrderStatusRequest(OrderStatus.CONFIRMED);
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        PageResponse pageResponse = PageResponse.from(new PageImpl<>(List.of(OrderResponse.from(order))));
        when(orderService.getOrderListByUserAndStatus(anyLong(), any(), any())).thenReturn(pageResponse);

        // when, then
        mockMvc.perform(get("/orders/me/status")
                        .header("X-User-Id",1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("날짜별 전체 주문 리스트 조회 - 성공")
    void getOrderListByDate() throws Exception {
        // given
        OrderDateRequest dateRequest = new OrderDateRequest(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 31, 23, 59)
        );
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        PageResponse pageResponse = PageResponse.from(new PageImpl<>(List.of(OrderResponse.from(order))));
        when(orderService.getOrderListByDate(any(), any())).thenReturn(pageResponse);

        // when, then
        mockMvc.perform(get("/orders/date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dateRequest))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 상태별 전체 주문 리스트 조회 - 성공")
    void getOrderListByStatus_성공() throws Exception {
        // given
        OrderStatusRequest statusRequest = new OrderStatusRequest(OrderStatus.CONFIRMED);
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        PageResponse pageResponse = PageResponse.from(new PageImpl<>(List.of(OrderResponse.from(order))));
        when(orderService.getOrderListByStatus(any(), any())).thenReturn(pageResponse);

        // when, then
        mockMvc.perform(post("/orders/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 전체 리스트 조회 - 성공")
    void getOrders_성공() throws Exception {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        OrderResponse orderResponse = OrderResponse.from(order);
        PageResponse<OrderResponse> orderResponsePageResponse
                = PageResponse.from(new PageImpl<>(List.of(orderResponse)));
        when(orderService.getOrderList(any())).thenReturn(orderResponsePageResponse);

        // when, then
        mockMvc.perform(MockMvcRequestBuilders.get("/orders")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andDo(print());
        verify(orderService).getOrderList(any());
    }

    @Test
    @DisplayName("주문 수정 - 성공")
    void updateOrder_성공() throws Exception {
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        OrderUpdateRequest orderUpdateRequest = new OrderUpdateRequest(
                "권용민", "010-9926-8961");
        when(orderService.updateOrder(anyLong(), any())).thenReturn(OrderResponse.from(order));

        mockMvc.perform(put("/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 수정 - 실패")
    void updateOrder_실패() throws Exception {
        //given
        OrderUpdateRequest orderUpdateRequest = new OrderUpdateRequest(
                "권용민", "010-9926-8961");
        doThrow(new OrderNotFoundException(1L)).when(orderService).updateOrder(anyLong(), any());

        // when, then
        mockMvc.perform(put("/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderUpdateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 상태 변경 - 성공")
    void setOrderStatus_성공() throws Exception {
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest(OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(anyLong(), any())).thenReturn(OrderResponse.from(order));

        mockMvc.perform(patch("/orders/1/order-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderStatusRequest)))
                .andExpect(status().is2xxSuccessful())
                .andDo(print());
    }

    @Test
    @DisplayName("주문 상태 변경 - 실패")
    void setOrderStatus_실패() throws Exception {
        // given
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest(OrderStatus.CONFIRMED);
        doThrow(new OrderNotFoundException(1L)).when(orderService).updateOrderStatus(anyLong(), any());

        // when, then
        mockMvc.perform(patch("/orders/1/order-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderStatusRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 삭제 - 성공")
    void deleteOrder_성공() throws Exception {
        doNothing().when(orderService).deleteOrder(anyLong());

        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().is2xxSuccessful())
                .andDo(print());
    }

    @Test
    @DisplayName("주문 삭제 - 실패")
    void deleteOrder_실패() throws Exception {
        doThrow(new OrderNotFoundException(1L)).when(orderService).deleteOrder(anyLong());

        mockMvc.perform(delete("/orders/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}