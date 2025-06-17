package shop.ink3.api.order.refund.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.order.refund.dto.RefundCreateRequest;
import shop.ink3.api.order.refund.dto.RefundResponse;
import shop.ink3.api.order.refund.dto.RefundUpdateRequest;
import shop.ink3.api.order.refund.entity.Refund;
import shop.ink3.api.order.refund.exception.RefundNotFoundException;
import shop.ink3.api.order.refund.repository.RefundRepository;
import shop.ink3.api.user.user.entity.User;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    RefundRepository refundRepository;

    @InjectMocks
    RefundService refundService;

    @Mock
    OrderRepository orderRepository;

    @Test
    @DisplayName("반품 조회 - 성공")
    void getRefund_성공() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        RefundCreateRequest request = new RefundCreateRequest(1L, "테스트 사유", "테스트 상세",3000, LocalDateTime.now(), true);
        Refund refund = Refund.builder()
                .id(1L)
                .order(order)
                .reason(request.getReason())
                .details(request.getDetails())
                .build();
        when(refundRepository.findByOrderId(anyLong())).thenReturn(Optional.of(refund));

        // when
        RefundResponse response = refundService.getOrderRefund(1L);

        // then
        assertNotNull(response);
        assertEquals(1, response.getId());
    }

    @Test
    @DisplayName("반품 조회 - 실패")
    void getRefund_실패() {
        // given
        when(refundRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(RefundNotFoundException.class,
                () -> refundService.getOrderRefund(1L));
    }

    @Test
    @DisplayName("사용자의 반품 리스트 조회 - 성공")
    void getUserRefundList_성공() {
        // given
        User user1 = User.builder().id(1L).build();
        Order order1 = Order.builder().id(1L).user(user1).build();
        User user2 = User.builder().id(1L).build();
        Order order2 = Order.builder().id(1L).user(user2).build();
        List<Refund> policies = List.of(
                Refund.builder().id(1L).order(order1).build(),
                Refund.builder().id(2L).order(order2).build()
        );
        Pageable pageable = PageRequest.of(0, 2);
        Page<Refund> page = new PageImpl<>(policies, pageable, policies.size());
        when(refundRepository.findAllByOrderUserId(anyLong(), any())).thenReturn(page);

        // when
        PageResponse<RefundResponse> pageResponse = refundService.getUserRefundList(1L, pageable);

        // then
        assertNotNull(pageResponse);
        assertEquals(2, pageResponse.content().size());
        assertEquals(1, pageResponse.content().get(0).getId());
        assertEquals(2, pageResponse.content().get(1).getId());
    }

    @Test
    @DisplayName("사용자의 반품 리스트 조회 - 성공")
    void createRefund_성공() {
        // given
        RefundCreateRequest request = new RefundCreateRequest(1L, "테스트 사유", "테스트 상세",3000, LocalDateTime.now(), true);
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        Refund refund = Refund.builder()
                .id(1L)
                .order(order)
                .reason(request.getReason())
                .details(request.getDetails())
                .build();
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(order));
        when(refundRepository.save(any())).thenReturn(refund);

        // when
        RefundResponse saveRefund = refundService.createRefund(request);

        // then
        assertNotNull(saveRefund);
        assertEquals(1, saveRefund.getId());
    }

    @Test
    @DisplayName("사용자의 반품 리스트 조회 - 성공")
    void updateRefund_성공() {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        RefundUpdateRequest request = new RefundUpdateRequest("변경후 사유", "변경후 상세");
        Refund refund = Refund.builder()
                .id(1L)
                .order(order)
                .reason("변경전 사유")
                .details("변경전 상세")
                .build();
        when(refundRepository.findByOrderId(anyLong())).thenReturn(Optional.of(refund));
        when(refundRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        RefundResponse saveRefund = refundService.updateRefund(anyLong(), request);

        // then
        assertNotNull(saveRefund);
        assertEquals(1, saveRefund.getId());
        assertEquals("변경후 사유", saveRefund.getReason());
        assertEquals("변경후 상세", saveRefund.getDetails());
    }

    @Test
    @DisplayName("반품 수정 - 실패")
    void updateRefund_실패() {
        // given
        RefundUpdateRequest request = new RefundUpdateRequest("변경후 사유", "변경후 상세");
        when(refundRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(RefundNotFoundException.class,
                () -> refundService.updateRefund(anyLong(), request));
    }

    @Test
    @DisplayName("반품 삭제 - 성공")
    void deleteRefund_성공() {
        // given
        Refund refund = Refund.builder()
                .id(1L)
                .reason("테스트 사유")
                .details("테스트 상세")
                .build();
        when(refundRepository.findByOrderId(anyLong())).thenReturn(Optional.of(refund));

        // when
        refundService.deleteRefund(1L);

        // then
        verify(refundRepository).deleteById(1L);
    }

    @Test
    @DisplayName("반품 삭제 - 실패")
    void deleteRefund_실패() {
        // given
        when(refundRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());

        // when, then
        assertThrows(RefundNotFoundException.class,
                () -> refundService.deleteRefund(1L));
    }
}
