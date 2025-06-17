package shop.ink3.api.order.guest.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.ink3.api.order.guest.dto.GuestCreateRequest;
import shop.ink3.api.order.guest.dto.GuestResponse;
import shop.ink3.api.order.guest.entiity.Guest;
import shop.ink3.api.order.guest.exception.GuestOrderNotFoundException;
import shop.ink3.api.order.guest.repository.GuestOrderRepository;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.exception.OrderNotFoundException;
import shop.ink3.api.order.order.repository.OrderRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GuestServiceTest {

    @Mock
    private GuestOrderRepository guestOrderRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GuestService guestService;

    public GuestServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("createGuest")
    class CreateGuest {
        @Test
        void success() {
            GuestCreateRequest request = new GuestCreateRequest("test@example.com");
            Order order = Order.builder().id(1L).build();
            Guest guest = Guest.builder().id(1L).order(order).email("test@example.com").build();

            when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
            when(guestOrderRepository.save(any(Guest.class))).thenReturn(guest);

            GuestResponse response = guestService.createGuest(1L, request);

            assertThat(response.orderId()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("test@example.com");
        }

        @Test
        void fail_orderNotFound() {
            GuestCreateRequest request = new GuestCreateRequest("test@example.com");
            when(orderRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(OrderNotFoundException.class,
                    () -> guestService.createGuest(1L, request));
        }
    }

    @Nested
    @DisplayName("getGuestByOrderId")
    class GetGuest {
        @Test
        void success() {
            Order order = Order.builder().id(1L).build();
            Guest guest = Guest.builder().id(1L).order(order).email("test@example.com").build();

            when(guestOrderRepository.findByOrderId(1L)).thenReturn(Optional.of(guest));

            GuestResponse response = guestService.getGuestByOrderId(1L);

            assertThat(response.orderId()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("test@example.com");
        }

        @Test
        void fail_guestNotFound() {
            when(guestOrderRepository.findByOrderId(1L)).thenReturn(Optional.empty());

            assertThrows(GuestOrderNotFoundException.class,
                    () -> guestService.getGuestByOrderId(1L));
        }
    }

    @Nested
    @DisplayName("deleteGuestOrderByOrderId")
    class DeleteGuest {
        @Test
        void success() {
            Order order = Order.builder().id(1L).build();
            Guest guest = Guest.builder().id(1L).order(order).email("test@example.com").build();

            when(guestOrderRepository.findByOrderId(1L)).thenReturn(Optional.of(guest));
            doNothing().when(guestOrderRepository).deleteByOrderId(1L);

            guestService.deleteGuestOrderByOrderId(1L);

            verify(guestOrderRepository).deleteByOrderId(1L);
        }

        @Test
        void fail_guestNotFound() {
            when(guestOrderRepository.findByOrderId(1L)).thenReturn(Optional.empty());

            assertThrows(GuestOrderNotFoundException.class,
                    () -> guestService.deleteGuestOrderByOrderId(1L));
        }
    }
}