package shop.ink3.api.order.guest.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.ink3.api.order.guest.dto.GuestOrderCreateRequest;
import shop.ink3.api.order.guest.dto.GuestOrderDetailsResponse;
import shop.ink3.api.order.guest.dto.GuestOrderResponse;
import shop.ink3.api.order.guest.exception.GuestOrderNotFoundException;
import shop.ink3.api.order.guest.repository.GuestOrderRepository;
import shop.ink3.api.order.order.dto.OrderStatusUpdateRequest;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.order.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuestOrderServiceTest {

    @Mock
    private GuestOrderRepository guestOrderRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GuestOrderService guestOrderService;

    public GuestOrderServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("비회원 주문 생성 성공")
    void createGuestOrder_success() {
        GuestOrderCreateRequest request = new GuestOrderCreateRequest("홍길동", "010-1234-5678");
        Order mockOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .ordererName(request.getOrdererName())
                .ordererPhone(request.getOrdererPhone())
                .orderedAt(LocalDateTime.now())
                .build();

        when(orderRepository.save(any(Order.class)))
                .thenReturn(mockOrder);

        GuestOrderResponse response = guestOrderService.createGuestOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("비회원 주문 상세 조회 성공")
    void getGuestOrderDetails_success() {
        GuestOrderDetailsResponse mockResponse = mock(GuestOrderDetailsResponse.class);
        when(guestOrderRepository.findByGuestOrderDetails(1L)).thenReturn(Optional.of(mockResponse));

        GuestOrderDetailsResponse response = guestOrderService.getGuestOrderDetails(1L);

        assertThat(response).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("비회원 주문 상세 조회 실패 - 존재하지 않음")
    void getGuestOrderDetails_notFound() {
        when(guestOrderRepository.findByGuestOrderDetails(1L)).thenReturn(Optional.empty());

        assertThrows(GuestOrderNotFoundException.class, () ->
                guestOrderService.getGuestOrderDetails(1L)
        );
    }

    @Test
    @DisplayName("주문 상태 변경 성공")
    void updateOrderStatus_success() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        GuestOrderResponse response = guestOrderService.updateOrderStatus(1L, request);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("주문 상태 변경 실패 - 주문 없음")
    void updateOrderStatus_notFound() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () ->
                guestOrderService.updateOrderStatus(1L, request)
        );
    }
}
