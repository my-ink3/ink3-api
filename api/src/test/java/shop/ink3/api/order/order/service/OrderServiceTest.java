package shop.ink3.api.order.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.order.order.dto.OrderCreateRequest;
import shop.ink3.api.order.order.dto.OrderDateRequest;
import shop.ink3.api.order.order.dto.OrderResponse;
import shop.ink3.api.order.order.dto.OrderStatusRequest;
import shop.ink3.api.order.order.dto.OrderStatusUpdateRequest;
import shop.ink3.api.order.order.dto.OrderUpdateRequest;
import shop.ink3.api.order.order.dto.OrderWithDetailsResponse;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.exception.UserNotFoundException;
import shop.ink3.api.user.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 - 성공")
    void createOrder_성공() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(
                1L,
                "권용민",
                "010-9926-8961");
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        OrderResponse orderResponse = orderService.createOrder(request);

        // then
        assertNotNull(orderResponse);
        assertEquals(order.getId(), orderResponse.getId());

        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("주문 생성 - 실패")
    void createOrder_실패() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(
                1L,
                "권용민",
                "010-9926-8961");
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(UserNotFoundException.class, () -> orderService.createOrder(request));
    }

    @Test
    @DisplayName("주문 조회 - 성공")
    void getOrder_성공() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));

        // when
        OrderResponse orderResponse = orderService.getOrder(order.getId());

        // then
        assertEquals(order.getId(), orderResponse.getId());
    }

    @Test
    @DisplayName("주문 조회 - 실패")
    void getOrder_실패() {
        // given
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrder(1L));
    }

    @Test
    @DisplayName("사용자의 주문 리스트 조회 - 성공")
    void getOrderListByUser_성공() {
        // given
        User user = User.builder().id(1L).build();
        OrderWithDetailsResponse mockResponse1 = mock(OrderWithDetailsResponse.class);
        OrderWithDetailsResponse mockResponse2 = mock(OrderWithDetailsResponse.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderWithDetailsResponse> mockResponsePage = new PageImpl<>(List.of(mockResponse1, mockResponse2));
        when(orderRepository.findAllByUserId(user.getId(), pageable)).thenReturn(mockResponsePage);

        // when
        PageResponse<OrderWithDetailsResponse> orderListByUser = orderService.getOrderListByUser(user.getId(), pageable);

        // then
        assertNotNull(orderListByUser);
        assertEquals(orderListByUser.size(), mockResponsePage.getTotalElements());
    }

    @Test
    @DisplayName("사용자의 주문상태별 주문 리스트 조회 - 성공")
    void getOrderListByUserAndStatus_success() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).status(OrderStatus.CONFIRMED).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderResponsePage = new PageImpl<>(List.of(order));
        when(orderRepository.findAllByUserIdAndStatus(user.getId(), OrderStatus.CONFIRMED, pageable))
                .thenReturn(orderResponsePage);

        // when
        PageResponse<OrderResponse> orderListByUserAndStatus = orderService.getOrderListByUserAndStatus(
                user.getId(), new OrderStatusRequest(OrderStatus.CONFIRMED), pageable);

        // then
        assertNotNull(orderListByUserAndStatus);
        assertEquals(1, orderListByUserAndStatus.size());

        verify(orderRepository).findAllByUserIdAndStatus(user.getId(), OrderStatus.CONFIRMED, pageable);
    }

    @Test
    @DisplayName("사용자의 기간별 주문 리스트 조회 - 성공")
    void getOrderListByUserAndDate_success() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).orderedAt(LocalDateTime.now()).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderResponsePage = new PageImpl<>(List.of(order));
        when(orderRepository.findAllByUserIdAndOrderedAtBetween(eq(user.getId()), any(), any(), eq(pageable)))
                .thenReturn(orderResponsePage);

        // when
        OrderDateRequest orderDateRequest = new OrderDateRequest(LocalDateTime.now(),LocalDateTime.now());
        PageResponse<OrderResponse> orderListByUserAndDate
                = orderService.getOrderListByUserAndDate(user.getId(), orderDateRequest, pageable);

        // then
        assertNotNull(orderListByUserAndDate);
        assertEquals(1, orderListByUserAndDate.size());

        verify(orderRepository).findAllByUserIdAndOrderedAtBetween(
                eq(user.getId()), any(), any(), eq(pageable)
        );
    }

    @Test
    @DisplayName("기간별 전체 주문 리스트 조회 - 성공")
    void getOrderListByDate_success() {
        // given
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .orderedAt(LocalDateTime.of(2025, 6, 15, 19, 41))
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));
        OrderDateRequest request = new OrderDateRequest(
                LocalDateTime.of(2025, 6, 1, 0, 0),
                LocalDateTime.of(2025, 6, 30, 23, 59)
        );
        when(orderRepository.findAllByOrderedAtBetween(request.getStartDate(), request.getEndDate(), pageable))
                .thenReturn(page);

        // when
        PageResponse<OrderResponse> result = orderService.getOrderListByDate(request, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findAllByOrderedAtBetween(request.getStartDate(), request.getEndDate(), pageable);
    }

    @Test
    @DisplayName("전체 주문 리스트 조회 - 성공")
    void getOrderList_success() {
        // given
        Order order = Order.builder().id(1L).status(OrderStatus.CREATED).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(pageable)).thenReturn(page);

        // when
        PageResponse<OrderResponse> result = orderService.getOrderList(pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findAll(pageable);
    }

    @Test
    @DisplayName("주문상태별 전체 주문 리스트 조회 - 성공")
    void getOrderListByStatus_success() {
        // given
        Order order = Order.builder().id(1L).status(OrderStatus.CANCELLED).build();
        Pageable pageable = PageRequest.of(0, 10);
        OrderStatusRequest request = new OrderStatusRequest(OrderStatus.CANCELLED);
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findAllByStatus(request.getOrderStatus(), pageable)).thenReturn(page);

        // when
        PageResponse<OrderResponse> result = orderService.getOrderListByStatus(request, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findAllByStatus(request.getOrderStatus(), pageable);
    }

    @Test
    @DisplayName("주문 수정 - 성공")
    void updateOrder_성공() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        OrderUpdateRequest request = new OrderUpdateRequest("권용민", "010-9926-8961");
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        OrderResponse orderResponse = orderService.updateOrder(order.getId(), request);

        // then
        assertNotNull(orderResponse);
        assertEquals(order.getId(), orderResponse.getId());
    }

    @Test
    @DisplayName("주문 수정 - 실패")
    void updateOrder_실패() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        OrderUpdateRequest request = new OrderUpdateRequest("권용민", "010-9926-8961");
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(OrderNotFoundException.class, ()->orderService.updateOrder(order.getId(), request));
    }

    @Test
    @DisplayName("주문 삭제 - 성공")
    void deleteOrder_성공() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        doNothing().when(orderRepository).deleteById(anyLong());

        // when
        orderService.deleteOrder(order.getId());

        // then
        verify(orderRepository).deleteById(order.getId());
    }

    @Test
    @DisplayName("주문 삭제 - 실패")
    void deleteOrder_실패() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(OrderNotFoundException.class, ()->orderService.deleteOrder(order.getId()));
    }

    @Test
    @DisplayName("주문 상태 변경 - 성공")
    void updateOrderStatus_성공() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        OrderStatusUpdateRequest orderStatusUpdateRequest = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);

        // when
        OrderResponse orderResponse = orderService.updateOrderStatus(order.getId(), orderStatusUpdateRequest);

        // then
        assertNotNull(orderResponse);
        assertEquals(order.getId(), orderResponse.getId());
    }

    @Test
    @DisplayName("주문 상태 변경 - 실패")
    void updateOrderStatus_실패() {
        // given
        OrderStatusUpdateRequest orderStatusUpdateRequest = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(OrderNotFoundException.class, ()->orderService.updateOrderStatus(1L, orderStatusUpdateRequest));
    }
}