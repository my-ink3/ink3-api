package shop.ink3.api.order.guest.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shop.ink3.api.order.guest.dto.GuestCreateRequest;
import shop.ink3.api.order.guest.dto.GuestOrderCreateRequest;
import shop.ink3.api.order.guest.dto.GuestOrderFormCreateRequest;
import shop.ink3.api.order.guest.dto.GuestOrderResponse;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.orderBook.dto.OrderBookCreateRequest;
import shop.ink3.api.order.orderBook.service.OrderBookService;
import shop.ink3.api.order.shipment.dto.ShipmentCreateRequest;
import shop.ink3.api.order.shipment.service.ShipmentService;
import shop.ink3.api.payment.entity.PaymentType;

class GuestOrderMainServiceTest {

    @Mock private GuestOrderService guestOrderService;
    @Mock private GuestService guestService;
    @Mock private OrderBookService orderBookService;
    @Mock private ShipmentService shipmentService;

    @InjectMocks private GuestOrderMainService guestOrderMainService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("비회원 주문서 생성 성공")
    void createGuestOrderForm_success() {
        // given
        GuestOrderCreateRequest orderReq = new GuestOrderCreateRequest(
            "홍길동",
            "010-1234-5678"
        );
        GuestCreateRequest guestReq = new GuestCreateRequest(
            "test@gmail.com"
        );
        ShipmentCreateRequest shipmentReq = new ShipmentCreateRequest(
            LocalDate.of(2025, 6, 18),
            "홍길동",
            "010-1234-5678",               // recipientPhone
            12345,                         // postalCode
            "서울시 강남구 테헤란로 1",       // defaultAddress
            "101동 202호",                  // detailAddress
            "역삼동",                      // extraAddress
            2500,                          // shippingFee
            "CJ123456789"                 // shippingCode
        );
        List<OrderBookCreateRequest> books = List.of();

        GuestOrderFormCreateRequest request = new GuestOrderFormCreateRequest(
            guestReq,
            orderReq,
            shipmentReq,
            books,
            15000,
            PaymentType.TOSS
        );
        GuestOrderResponse expected = new GuestOrderResponse(
            1L,
            "uuid-abc",
            OrderStatus.CONFIRMED,
            LocalDateTime.of(2025, 6, 17, 12, 0),
            "홍길동",
            "010-1234-5678"
        );

        when(guestOrderService.createGuestOrder(orderReq)).thenReturn(expected);

        // when
        GuestOrderResponse result = guestOrderMainService.createGuestOrderForm(request);

        // then
        assertThat(result).isEqualTo(expected);
        verify(guestService).createGuest(1L, guestReq);
        verify(orderBookService).createOrderBook(1L, books);
        verify(shipmentService).createShipment(1L, shipmentReq);
    }
}
