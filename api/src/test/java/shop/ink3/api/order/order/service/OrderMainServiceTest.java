package shop.ink3.api.order.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import shop.ink3.api.coupon.store.dto.CouponStoreUpdateRequest;
import shop.ink3.api.coupon.store.service.CouponStoreService;
import shop.ink3.api.order.order.dto.OrderFormCreateRequest;
import shop.ink3.api.order.order.dto.OrderResponse;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.orderBook.service.OrderBookService;
import shop.ink3.api.order.orderPoint.entity.OrderPoint;
import shop.ink3.api.order.orderPoint.service.OrderPointService;
import shop.ink3.api.order.refund.dto.RefundResponse;
import shop.ink3.api.order.refund.service.RefundService;
import shop.ink3.api.order.shipment.service.ShipmentService;
import shop.ink3.api.payment.dto.PaymentResponse;
import shop.ink3.api.payment.service.PaymentService;
import shop.ink3.api.user.point.history.entity.PointHistory;
import shop.ink3.api.user.point.history.service.PointService;
import shop.ink3.api.user.user.dto.UserPointRequest;

class OrderMainServiceTest {

    @Mock private OrderService orderService;
    @Mock private OrderBookService orderBookService;
    @Mock private ShipmentService shipmentService;
    @Mock private RefundService refundService;
    @Mock private PaymentService paymentService;
    @Mock private PointService pointService;
    @Mock private OrderPointService orderPointService;
    @Mock private CouponStoreService couponStoreService;

    @InjectMocks private OrderMainService orderMainService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("주문서 생성 성공")
    void createOrderForm_success() {
        OrderFormCreateRequest request = mock(OrderFormCreateRequest.class);
        OrderResponse response = new OrderResponse(1L, 1L, "AAA", OrderStatus.CREATED, LocalDateTime.now(), "test", "010-1234-5678");

        when(request.orderCreateRequest()).thenReturn(null);
        when(request.createRequestList()).thenReturn(List.of());
        when(request.shipmentCreateRequest()).thenReturn(null);
        when(orderService.createOrder(null)).thenReturn(response);

        OrderResponse result = orderMainService.createOrderForm(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(shipmentService).createShipment(1L, null);
    }

    @Test
    @DisplayName("반품 승인 성공")
    void approveRefund_success() {
        long orderId = 10L;
        long userId = 5L;

        RefundResponse refund = new RefundResponse(99L, orderId, "reason", "detail", 1000, LocalDateTime.now(), true, 1L);
        PaymentResponse payment = new PaymentResponse(1L, orderId, "paykey", 0, 0, 10000, null, null, null);

        PointHistory mockHistory = mock(PointHistory.class);
        when(mockHistory.getId()).thenReturn(777L);

        OrderPoint orderPoint = mock(OrderPoint.class);
        when(orderPoint.getPointHistory()).thenReturn(mockHistory);

        when(refundService.updateApproved(orderId)).thenReturn(refund);
        when(paymentService.getPayment(refund.getId())).thenReturn(payment);
        when(orderPointService.getOrderPoints(refund.getId())).thenReturn(List.of(orderPoint));
        when(orderBookService.getOrderCouponStoreId(orderId)).thenReturn(Optional.of(55L));

        orderMainService.approveRefund(userId, orderId);

        verify(pointService).earnPoint(eq(userId), any(UserPointRequest.class));
        verify(orderBookService).resetBookQuantity(orderId);
        verify(pointService).cancelPoint(userId, 777L);
        verify(couponStoreService).updateStore(eq(55L), any(CouponStoreUpdateRequest.class));
    }
}
