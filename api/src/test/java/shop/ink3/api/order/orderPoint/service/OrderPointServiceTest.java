package shop.ink3.api.order.orderPoint.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.order.orderPoint.entity.OrderPoint;
import shop.ink3.api.order.orderPoint.repository.OrderPointRepository;
import shop.ink3.api.user.point.history.entity.PointHistory;
import shop.ink3.api.user.point.history.repository.PointHistoryRepository;

class OrderPointServiceTest {

    @InjectMocks
    private OrderPointService orderPointService;

    @Mock
    private OrderPointRepository orderPointRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("정상적으로 OrderPoint 생성")
    @Test
    void createOrderPoint_success() {
        long orderId = 1L;
        Order order = Order.builder().id(orderId).build();
        PointHistory pointHistory = PointHistory.builder().id(100L).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderPointRepository.save(any(OrderPoint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderPoint result = orderPointService.createOrderPoint(orderId, pointHistory);

        assertThat(result.getOrder()).isEqualTo(order);
        assertThat(result.getPointHistory()).isEqualTo(pointHistory);
        verify(orderRepository).findById(orderId);
        verify(orderPointRepository).save(any(OrderPoint.class));
    }

    @DisplayName("주문이 존재하지 않으면 예외 발생")
    @Test
    void createOrderPoint_orderNotFound() {
        long orderId = 999L;
        PointHistory pointHistory = PointHistory.builder().id(100L).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderPointService.createOrderPoint(orderId, pointHistory))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @DisplayName("주문 ID로 OrderPoint 목록 조회")
    @Test
    void getOrderPoints_success() {
        long orderId = 1L;
        List<OrderPoint> mockList = List.of(
            OrderPoint.builder().id(1L).build(),
            OrderPoint.builder().id(2L).build()
        );

        when(orderPointRepository.findAllByOrderId(orderId)).thenReturn(mockList);

        List<OrderPoint> result = orderPointService.getOrderPoints(orderId);

        assertThat(result).hasSize(2);
        verify(orderPointRepository).findAllByOrderId(orderId);
    }
}
