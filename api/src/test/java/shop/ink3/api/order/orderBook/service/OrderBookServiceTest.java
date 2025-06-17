package shop.ink3.api.order.orderBook.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
import shop.ink3.api.coupon.store.entity.CouponStatus;
import shop.ink3.api.coupon.store.entity.CouponStore;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.coupon.store.exception.CouponInvalidPeriodException;
import shop.ink3.api.coupon.store.repository.CouponStoreRepository;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.repository.OrderRepository;
import shop.ink3.api.order.orderBook.dto.OrderBookCreateRequest;
import shop.ink3.api.order.orderBook.dto.OrderBookResponse;
import shop.ink3.api.order.orderBook.dto.OrderBookUpdateRequest;
import shop.ink3.api.order.orderBook.entity.OrderBook;
import shop.ink3.api.order.orderBook.exception.OrderBookNotFoundException;
import shop.ink3.api.order.orderBook.repository.OrderBookRepository;
import shop.ink3.api.order.packaging.entity.Packaging;
import shop.ink3.api.order.packaging.repository.PackagingRepository;
import shop.ink3.api.user.user.entity.User;

class OrderBookServiceTest {

    @InjectMocks
    private OrderBookService orderBookService;

    @Mock private OrderBookRepository orderBookRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private BookRepository bookRepository;
    @Mock private PackagingRepository packagingRepository;
    @Mock private CouponStoreRepository couponStoreRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrderBook_success() {
        Order order = Order.builder().id(1L).build();
        Book book = Book.builder().id(1L).quantity(10).title("test").build();
        CouponPolicy couponPolicy = mock(CouponPolicy.class);
        Coupon coupon = Coupon.builder()
                .id(1L)
                .couponPolicy(couponPolicy)
                .name("WELCOME")
                .issuableFrom(LocalDateTime.now().plusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        User user = User.builder().id(1L).build();
        CouponStore couponStore = CouponStore.builder()
                .id(1L)
                .user(user)
                .coupon(coupon)
                .originType(OriginType.WELCOME)
                .originId(1L)
                .status(CouponStatus.READY)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .build();
        Packaging packaging = Packaging.builder().id(1L).build();

        OrderBookCreateRequest req = new OrderBookCreateRequest(1L, 1L, 1L, 1000, 2);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(packagingRepository.findById(1L)).thenReturn(Optional.of(packaging));
        when(couponStoreRepository.findById(1L)).thenReturn(Optional.of(couponStore));

        orderBookService.createOrderBook(1L, List.of(req));

        verify(orderBookRepository).save(any(OrderBook.class));
    }

    @Test
    void createOrderBook_fail_couponInvalidPeriod() {
        Order order = Order.builder().id(1L).build();
        Book book = Book.builder().id(1L).quantity(10).title("test").build();
        CouponPolicy couponPolicy = mock(CouponPolicy.class);
        Coupon coupon = Coupon.builder()
                .id(1L)
                .couponPolicy(couponPolicy)
                .name("WELCOME")
                .issuableFrom(LocalDateTime.now().plusDays(3))  // 미래
                .expiresAt(LocalDateTime.now().minusDays(1))    // 과거
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        User user = User.builder().id(1L).build();
        CouponStore couponStore = CouponStore.builder()
                .id(1L)
                .user(user)
                .coupon(coupon)
                .originType(OriginType.WELCOME)
                .originId(1L)
                .status(CouponStatus.READY)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .build();
        Packaging packaging = Packaging.builder().id(1L).build();

        OrderBookCreateRequest req = new OrderBookCreateRequest(1L, 1L, 1L, 1000, 2);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(packagingRepository.findById(1L)).thenReturn(Optional.of(packaging));
        when(couponStoreRepository.findById(1L)).thenReturn(Optional.of(couponStore));

        assertThatThrownBy(() -> orderBookService.createOrderBook(1L, List.of(req)))
                .isInstanceOf(CouponInvalidPeriodException.class);
    }

    @Test
    void getOrderBook_success() {
        Order order = Order.builder().id(1L).build();
        Book book = Book.builder().id(1L).quantity(10).title("test").build();
        OrderBook ob = OrderBook.builder()
                .id(1L)
                .order(order)
                .book(book)
                .couponStore(null)
                .packaging(null)
                .price(30000)
                .quantity(2)
                .build();
        when(orderBookRepository.findById(1L)).thenReturn(Optional.of(ob));

        OrderBookResponse response = orderBookService.getOrderBook(1L);
        assertThat(response).isNotNull();
    }

    @Test
    void getOrderBook_fail_notFound() {
        when(orderBookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderBookService.getOrderBook(1L))
                .isInstanceOf(OrderBookNotFoundException.class);
    }

    @Test
    void getOrderBookListByOrderId_success() {
        Order order = Order.builder().id(1L).build();
        Book book = Book.builder().id(1L).quantity(10).title("test").build();
        OrderBook ob = OrderBook.builder()
                .id(1L)
                .order(order)
                .book(book)
                .couponStore(null)
                .packaging(null)
                .price(30000)
                .quantity(2)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderBook> page = new PageImpl<>(List.of(ob), pageable, 1);

        when(orderBookRepository.findAllByOrderId(1L, pageable)).thenReturn(page);
        var result = orderBookService.getOrderBookListByOrderId(1L, pageable);
        assertThat(result).isNotNull();
        assertThat(result.content().size()).isEqualTo(1);
    }

    @Test
    void getOrderCouponStoreId_withCoupon() {
        CouponStore cs = mock(CouponStore.class);
        when(cs.getId()).thenReturn(10L);
        OrderBook ob = mock(OrderBook.class);
        when(ob.getCouponStore()).thenReturn(cs);

        when(orderBookRepository.findAllByOrderId(1L)).thenReturn(List.of(ob));
        Optional<Long> result = orderBookService.getOrderCouponStoreId(1L);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(10L);
    }

    @Test
    void updateOrderBook_success() {
        Order order = Order.builder().id(1L).build();
        Book book = Book.builder().id(1L).quantity(10).title("test").build();
        OrderBook orderBook = OrderBook.builder()
                .id(1L)
                .order(order)
                .book(book)
                .couponStore(null)
                .packaging(null)
                .price(30000)
                .quantity(2)
                .build();
        Packaging packaging = Packaging.builder().id(1L).build();
        CouponPolicy couponPolicy = mock(CouponPolicy.class);
        Coupon coupon = Coupon.builder()
                .id(1L)
                .couponPolicy(couponPolicy)
                .name("WELCOME")
                .issuableFrom(LocalDateTime.now().plusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        User user = User.builder().id(1L).build();
        CouponStore couponStore = CouponStore.builder()
                .id(1L)
                .user(user)
                .coupon(coupon)
                .originType(OriginType.WELCOME)
                .originId(1L)
                .status(CouponStatus.READY)
                .issuedAt(LocalDateTime.now())
                .usedAt(null)
                .build();
        OrderBookUpdateRequest request = new OrderBookUpdateRequest(1L, 1L, 30000, 2);

        when(orderBookRepository.findById(1L)).thenReturn(Optional.of(orderBook));
        when(packagingRepository.findById(1L)).thenReturn(Optional.of(packaging));
        when(couponStoreRepository.findById(1L)).thenReturn(Optional.of(couponStore));
        when(orderBookRepository.save(orderBook)).thenReturn(orderBook);

        var result = orderBookService.updateOrderBook(1L, request);
        assertThat(result).isNotNull();
    }

    @Test
    void deleteOrderBook_success() {
        OrderBook orderBook = mock(OrderBook.class);
        when(orderBookRepository.findById(1L)).thenReturn(Optional.of(orderBook));

        orderBookService.deleteOrderBook(1L);
        verify(orderBookRepository).deleteById(1L);
    }

    @Test
    void resetBookQuantity_success() {
        Order order = Order.builder().id(1L).build();
        Book book = Book.builder().id(5L).quantity(3).build();
        OrderBook ob = OrderBook.builder().book(book).quantity(2).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderBookRepository.findAllByOrderId(1L)).thenReturn(List.of(ob));
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book));

        orderBookService.resetBookQuantity(1L);
        verify(bookRepository).save(any(Book.class));
    }
}
