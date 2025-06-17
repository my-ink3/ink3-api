package shop.ink3.api.payment.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import shop.ink3.api.coupon.store.dto.CouponStoreUpdateRequest;
import shop.ink3.api.coupon.store.service.CouponStoreService;
import shop.ink3.api.order.order.dto.OrderResponse;
import shop.ink3.api.order.order.dto.OrderStatusUpdateRequest;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.order.order.service.OrderService;
import shop.ink3.api.order.orderBook.service.OrderBookService;
import shop.ink3.api.order.orderPoint.service.OrderPointService;
import shop.ink3.api.payment.dto.*;
import shop.ink3.api.payment.entity.Payment;
import shop.ink3.api.payment.entity.PaymentType;
import shop.ink3.api.payment.exception.*;
import shop.ink3.api.payment.paymentUtil.parser.PaymentParser;
import shop.ink3.api.payment.paymentUtil.processor.PaymentProcessor;
import shop.ink3.api.payment.paymentUtil.resolver.PaymentProcessorResolver;
import shop.ink3.api.payment.paymentUtil.resolver.PaymentResponseParserResolver;
import shop.ink3.api.payment.repository.PaymentRepository;
import shop.ink3.api.user.point.history.eventListener.PointHistoryAfterPaymentEven;
import shop.ink3.api.user.point.history.service.PointService;
import shop.ink3.api.user.user.dto.UserPointRequest;
import shop.ink3.api.user.user.entity.User;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private OrderBookService orderBookService;
    @Mock
    private OrderPointService orderPointService;
    @Mock
    private PointService pointService;
    @Mock
    private CouponStoreService couponStoreService;
    @Mock
    private PaymentProcessorResolver processorResolver;
    @Mock
    private PaymentResponseParserResolver parserResolver;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final long orderId = 1L;
    private final long userId = 2L;

    @Test
    @DisplayName("결제 조회 - 성공")
    void getPayment_성공() {
        // given
        Order order = Order.builder().id(1L).build();
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .paymentKey("abcde")
                .usedPoint(0)
                .discountPrice(0)
                .paymentAmount(1000)
                .paymentType(PaymentType.TOSS)
                .requestAt(LocalDateTime.now())
                .requestAt(LocalDateTime.now())
                .build();
        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(payment));

        // when
        PaymentResponse paymentResponse = paymentService.getPayment(orderId);

        // then
        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.orderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("반품 조회 - 실패")
    void getPayment_실패() {
        // given
        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> paymentService.getPayment(orderId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("결제 삭제 - 성공")
    void deletePayment_성공() {
        // given
        when(orderRepository.findById(orderId)).thenReturn(
                Optional.of(mock(shop.ink3.api.order.order.entity.Order.class)));

        // when
        paymentService.deletePayment(orderId);

        // then
        verify(paymentRepository).deleteByOrderId(orderId);
    }

    @Test
    @DisplayName("결제 삭제 - 실패")
    void deletePayment_실패() {
        // given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> paymentService.deletePayment(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("결제 생성 - 성공")
    void createPayment_성공() {
        Order order = Order.builder().id(1L).build();
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                orderId,
                userId,
                "abcdefg",
                "order-1-uuid",
                0,
                0,
                10000,
                PaymentType.TOSS);
        String responseBody = "mocked-response";
        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());

        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .paymentKey("abcde")
                .usedPoint(0)
                .discountPrice(0)
                .paymentAmount(1000)
                .paymentType(PaymentType.TOSS)
                .requestAt(LocalDateTime.now())
                .requestAt(LocalDateTime.now())
                .build();
        PaymentParser mockParser = mock(PaymentParser.class);
        when(parserResolver.getPaymentParser(any())).thenReturn(mockParser);
        when(orderService.updateOrderStatus(anyLong(), any())).thenReturn(OrderResponse.from(order));
        when(mockParser.paymentResponseParser((PaymentConfirmRequest) any(), anyString())).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponse response = paymentService.createPayment(request, responseBody);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        verify(eventPublisher).publishEvent(any(PointHistoryAfterPaymentEven.class));
    }

    @Test
    @DisplayName("결제 생성 - 실패")
    void createPayment_실패() {
        Order order = Order.builder().id(1L).build();
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .paymentKey("abcde")
                .usedPoint(0)
                .discountPrice(0)
                .paymentAmount(500)
                .paymentType(PaymentType.POINT)
                .requestAt(LocalDateTime.now())
                .requestAt(LocalDateTime.now())
                .build();
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                orderId,
                userId,
                "abcdefg",
                "order-1-uuid",
                0,
                0,
                10000,
                PaymentType.TOSS);
        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.createPayment(request, "irrelevant"))
                .isInstanceOf(PaymentAlreadyExistsException.class);
    }


    @Test
    @DisplayName("결제 실패 - 성공")
    void failPayment_성공() {
        when(orderBookService.getOrderCouponStoreId(orderId)).thenReturn(Optional.of(10L));

        paymentService.failPayment(orderId, userId);

        verify(orderBookService).resetBookQuantity(orderId);
        verify(orderService).updateOrderStatus(eq(orderId), any(OrderStatusUpdateRequest.class));
        verify(couponStoreService).updateStore(eq(10L), any(CouponStoreUpdateRequest.class));
    }

    @Test
    @DisplayName("결제 취소 - 성공")
    void cancelPayment_성공() {
        Order order = Order.builder().id(1L).build();
        Payment payment = Payment.builder()
                .id(1L)
                .order(order)
                .paymentKey("abcde")
                .usedPoint(0)
                .discountPrice(0)
                .paymentAmount(500)
                .paymentType(PaymentType.POINT)
                .requestAt(LocalDateTime.now())
                .requestAt(LocalDateTime.now())
                .build();

        OrderResponse orderResponse = mock(OrderResponse.class);
        when(orderResponse.getStatus()).thenReturn(OrderStatus.CONFIRMED);

        when(orderService.getOrder(orderId)).thenReturn(orderResponse);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(processorResolver.getPaymentProcessor("POINT-PROCESSOR")).thenReturn(mock(PaymentProcessor.class));
        when(orderPointService.getOrderPoints(orderId)).thenReturn(Collections.emptyList());

        PaymentCancelRequest cancelRequest = new PaymentCancelRequest(orderId, "abcde", 10000, PaymentType.POINT, "변심");
        paymentService.cancelPayment(orderId, userId, cancelRequest);

        verify(orderService).updateOrderStatus(eq(orderId), any());
        verify(pointService).earnPoint(eq(userId), any(UserPointRequest.class));
    }

    @Test
    @DisplayName("결제 취소 - 실패(대기상태 아닌 주문의 결제 취소)")
    void cancelPayment_실패_취소불가() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .status(OrderStatus.REFUNDED)
                .orderedAt(LocalDateTime.now())
                .orderUUID("order-1-uuid")
                .ordererName("권용민")
                .ordererPhone("010-9926-8961")
                .build();
        PaymentCancelRequest request = new PaymentCancelRequest(orderId, "abcde", 10000, PaymentType.POINT, "변심");
        when(orderService.getOrder(anyLong())).thenReturn(OrderResponse.from(order));

        // when, then
        assertThrows(PaymentCancelNotAllowedException.class,
                () -> paymentService.cancelPayment(orderId, userId, request));
    }

    @Test
    @DisplayName("결제 취소 - 실패(결제정보없음)")
    void cancelPayment_실패_결제정보없음() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder()
                .id(orderId)
                .user(user)
                .status(OrderStatus.CONFIRMED)
                .orderedAt(LocalDateTime.now())
                .orderUUID("order-1-uuid")
                .ordererName("권용민")
                .ordererPhone("010-9926-8961")
                .build();
        PaymentCancelRequest request = new PaymentCancelRequest(orderId, "abcde", 10000, PaymentType.POINT, "변심");
        when(orderService.getOrder(anyLong())).thenReturn(OrderResponse.from(order));
        when(paymentRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.cancelPayment(orderId, userId, request));
    }
}