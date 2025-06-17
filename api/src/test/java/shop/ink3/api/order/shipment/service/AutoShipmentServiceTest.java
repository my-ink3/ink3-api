package shop.ink3.api.order.shipment.service;

import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import shop.ink3.api.order.order.dto.OrderStatusUpdateRequest;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.order.service.OrderService;
import shop.ink3.api.order.shipment.dto.ShipmentResponse;

class AutoShipmentServiceTest {

    @InjectMocks
    private AutoShipmentService autoShipmentService;

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("CONFIRMED 상태의 주문을 SHIPPING 상태로 변경")
    @Test
    void autoBatchToSHIPPING_success() {
        ShipmentResponse response = new ShipmentResponse(
            1L,
            1L,
            LocalDate.now(),
            null,                     // deliveredAt
            "홍길동",                  // recipientName
            "010-1234-5678",         // recipientPhone
            12345,                   // postalCode
            "서울시 강남구",           // defaultAddress
            "101동 202호",            // detailAddress
            "역삼동",                 // extraAddress
            3000,                    // shippingFee
            "CJ123456789"            // shippingCode
        );
        when(shipmentService.getShipmentByOrderStatus(OrderStatus.CONFIRMED))
            .thenReturn(List.of(response));

        autoShipmentService.autoBatchToSHIPPING();

        verify(orderService).updateOrderStatus(eq(1L), any(OrderStatusUpdateRequest.class));
        verify(shipmentService).updateShipmentDeliveredAt(eq(1L), any(LocalDateTime.class));
    }

    @DisplayName("SHIPPING 상태의 주문을 DELIVERED 상태로 변경 (배송 예정일 당일)")
    @Test
    void autoBatchToDELIVERED_success() {
        ShipmentResponse response = new ShipmentResponse(
            1L,
            1L,
            LocalDate.now(),
            null,                     // deliveredAt
            "홍길동",                  // recipientName
            "010-1234-5678",         // recipientPhone
            12345,                   // postalCode
            "서울시 강남구",           // defaultAddress
            "101동 202호",            // detailAddress
            "역삼동",                 // extraAddress
            3000,                    // shippingFee
            "CJ123456789"            // shippingCode
        );
        when(shipmentService.getShipmentByOrderStatus(OrderStatus.SHIPPING))
            .thenReturn(List.of(response));

        autoShipmentService.autoBatchToDELIVERED();

        verify(orderService).updateOrderStatus(eq(1L), any(OrderStatusUpdateRequest.class));
        verify(shipmentService).updateShipmentDeliveredAt(eq(1L), any(LocalDateTime.class));
    }

    @DisplayName("배송 예정일이 오늘이 아닌 경우 DELIVERED 변경 안 함")
    @Test
    void autoBatchToDELIVERED_skip() {
        ShipmentResponse response = new ShipmentResponse(
            1L,
            1L,
            LocalDate.now().plusDays(1),
        null,                     // deliveredAt
            "홍길동",                  // recipientName
            "010-1234-5678",         // recipientPhone
            12345,                   // postalCode
            "서울시 강남구",           // defaultAddress
            "101동 202호",            // detailAddress
            "역삼동",                 // extraAddress
            3000,                    // shippingFee
            "CJ123456789"            // shippingCode
        );
        when(shipmentService.getShipmentByOrderStatus(OrderStatus.SHIPPING))
            .thenReturn(List.of(response));

        autoShipmentService.autoBatchToDELIVERED();

        verify(orderService, never()).updateOrderStatus(anyLong(), any());
        verify(shipmentService, never()).updateShipmentDeliveredAt(anyLong(), any());
    }
}
