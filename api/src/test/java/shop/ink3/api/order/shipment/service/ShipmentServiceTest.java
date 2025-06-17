package shop.ink3.api.order.shipment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
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
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.entity.OrderStatus;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.order.shipment.dto.*;
import shop.ink3.api.order.shipment.entity.Shipment;
import shop.ink3.api.order.shipment.exception.ShipmentNotFoundException;
import shop.ink3.api.order.shipment.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @InjectMocks ShipmentService shipmentService;
    @Mock
    OrderRepository orderRepository;

    @Test
    @DisplayName("배송 정보 조회 - 성공")
    void getShipment_성공() {
        // given
        Order order = Order.builder().id(1L).build();
        Shipment shipment = Shipment.builder().id(1L).order(order).build();
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.of(shipment));

        // when
        ShipmentResponse response = shipmentService.getShipment(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("배송 정보 조회 - 실패")
    void getShipment_실패() {
        // given
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        // when, then
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.getShipment(1L));
    }



    @Test
    @DisplayName("배송 상태에 따른 배송 정보 리스트 조회 - 성공")
    void getShipmentListByOrderStatus_성공() {
        // given
        Order order1 = Order.builder().id(1L).build();
        Order order2 = Order.builder().id(2L).build();
        Pageable pageable = PageRequest.of(0, 2);
        List<Shipment> list = List.of(
                Shipment.builder().id(1L).order(order1).build(),
                Shipment.builder().id(2L).order(order2).build()
        );
        Page<Shipment> page = new PageImpl<>(list, pageable, list.size());
        when(shipmentRepository.findAllByOrderUserIdAndOrderStatus(anyLong(),any(), any())).thenReturn(page);

        // when
        PageResponse<ShipmentResponse> response = shipmentService.getShipmentListByOrderStatus(1L, OrderStatus.SHIPPING, pageable);

        // then
        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("배송 생성 - 성공")
    void createShipment_성공() {
        ShipmentCreateRequest request = new ShipmentCreateRequest(
                LocalDate.now(),
                "수령인",
                "01012345678",
                12345,
                "주소1",
                "주소2",
                "주소3",
                3000,
                "CODE123"
        );
        Order order = Order.builder().id(1L).build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when
        ShipmentResponse response = shipmentService.createShipment(1L, request);

        // then
        assertNotNull(response);
        assertEquals("수령인", response.getRecipientName());
        assertEquals(1L, response.getOrderId());
    }

    @Test
    @DisplayName("배송 수정 - 성공")
    void updateShipment_성공() {
        // given
        Order order = Order.builder().id(1L).build();
        Shipment shipment = Shipment.builder().id(1L).order(order).build();
        ShipmentUpdateRequest request = new ShipmentUpdateRequest(
                "수령인",
                "01000000000",
                54321,
                "주소수정",
                "상세주소",
                "추가주소",
                5000,
                "CODE123"
        );
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any())).thenReturn(shipment);

        // when
        ShipmentResponse response = shipmentService.updateShipment(1L, request);

        // then
        assertNotNull(response);
        assertEquals("수령인", response.getRecipientName());
    }

    @Test
    @DisplayName("배송 수정 - 실패")
    void updateShipment_실패() {
        // given
        ShipmentUpdateRequest request = new ShipmentUpdateRequest(
                "수령인",
                "01000000000",
                54321,
                "주소수정",
                "상세주소",
                "추가주소",
                5000,
                "CODE123"
        );
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        // when, then
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.updateShipment(1L, request));
    }

    @Test
    @DisplayName("배송 삭제 - 성공")
    void deleteShipment_성공() {
        // given
        Shipment shipment = Shipment.builder().id(1L).build();
        when(shipmentRepository.findByOrderId(anyLong())).thenReturn(Optional.of(shipment));

        // when
        shipmentService.deleteShipment(1L);

        // then
        verify(shipmentRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("배송 삭제 - 실패")
    void deleteShipment_실패() {
        // given
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        // when, then
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.deleteShipment(1L));
    }

    @Test
    @DisplayName("배송 완료 시간 업데이트 - 성공")
    void updateShipmentDeliveredAt_성공() {
        // given
        Order order = Order.builder().id(1L).build();
        Shipment shipment = Shipment.builder().id(1L).order(order).build();
        LocalDateTime deliveredAt = LocalDateTime.now();
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any())).thenReturn(shipment);

        // when
        ShipmentResponse response = shipmentService.updateShipmentDeliveredAt(1L, deliveredAt);

        // then
        assertNotNull(response);
    }

    @Test
    @DisplayName("배송 완료 시간 업데이트 - 실패")
    void updateShipmentDeliveredAt_실패() {
        // given
        LocalDateTime deliveredAt = LocalDateTime.now();
        when(shipmentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        // when, then
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.updateShipmentDeliveredAt(1L, deliveredAt));
    }
}
