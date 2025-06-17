package shop.ink3.api.coupon.store.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.book.bookcategory.repository.BookCategoryRepository;
import shop.ink3.api.book.category.repository.CategoryRepository;
import shop.ink3.api.book.category.service.CategoryService;
import shop.ink3.api.coupon.bookCoupon.entity.BookCouponRepository;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCouponService;
import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.coupon.exception.CouponNotFoundException;
import shop.ink3.api.coupon.coupon.repository.CouponRepository;
import shop.ink3.api.coupon.store.dto.CouponIssueRequest;
import shop.ink3.api.coupon.store.entity.CouponStatus;
import shop.ink3.api.coupon.store.entity.CouponStore;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.coupon.store.exception.DuplicateCouponException;
import shop.ink3.api.coupon.store.repository.CouponStoreRepository;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
import shop.ink3.api.user.user.exception.UserNotFoundException;
import shop.ink3.api.user.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CouponStoreServiceTest {

    @InjectMocks
    private CouponStoreService couponStoreService;

    @Mock private CouponRepository couponRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookCouponRepository bookCouponRepository;
    @Mock private CategoryCouponService categoryCouponService;
    @Mock private CouponStoreRepository couponStoreRepository;
    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BookCategoryRepository bookCategoryRepository;
    @Mock private CategoryService categoryService;

    private User user;
    private Coupon coupon;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).build();
        coupon = Coupon.builder()
            .id(100L)
            .name("테스트쿠폰")
            .couponPolicy(CouponPolicy.builder().build())
            .issuableFrom(LocalDateTime.now().minusDays(1))
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();
    }

    @Nested
    @DisplayName("issueCoupon")
    class IssueCouponTest {

        @Test
        @DisplayName("정상적으로 쿠폰 발급")
        void issueCoupon_success() {
            CouponIssueRequest request = new CouponIssueRequest(coupon.getId(), OriginType.WELCOME, null);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(couponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));
            when(couponStoreRepository.existsByUserIdAndOriginType(user.getId(), request.originType())).thenReturn(false);
            when(userRepository.getReferenceById(user.getId())).thenReturn(user);
            when(couponRepository.getReferenceById(coupon.getId())).thenReturn(coupon);

            CouponStore result = couponStoreService.issueCoupon(request, user.getId());

            assertThat(result.getUser()).isEqualTo(user);
            assertThat(result.getCoupon()).isEqualTo(coupon);
            assertThat(result.getOriginType()).isEqualTo(OriginType.WELCOME);
            assertThat(result.getStatus()).isEqualTo(CouponStatus.READY);
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 예외 발생")
        void userNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> couponStoreService.issueCoupon(
                new CouponIssueRequest(100L, OriginType.WELCOME, null), 1L))
                .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("쿠폰이 존재하지 않으면 예외 발생")
        void couponNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(couponRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> couponStoreService.issueCoupon(
                new CouponIssueRequest(100L, OriginType.WELCOME, null), 1L))
                .isInstanceOf(CouponNotFoundException.class);
        }

        @Test
        @DisplayName("중복 발급이면 예외 발생 (originId 없음)")
        void duplicateCouponWithoutOriginId() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(couponRepository.findById(100L)).thenReturn(Optional.of(coupon));
            when(couponStoreRepository.existsByUserIdAndOriginType(1L, OriginType.WELCOME)).thenReturn(true);

            assertThatThrownBy(() -> couponStoreService.issueCoupon(
                new CouponIssueRequest(100L, OriginType.WELCOME, null), 1L))
                .isInstanceOf(DuplicateCouponException.class);
        }

        @Test
        @DisplayName("중복 발급이면 예외 발생 (originId 존재)")
        void duplicateCouponWithOriginId() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(couponRepository.findById(100L)).thenReturn(Optional.of(coupon));
            when(couponStoreRepository.existsByUserIdAndCouponIdAndOriginTypeAndOriginId(
                1L, 100L, OriginType.BOOK, 99L)).thenReturn(true);

            assertThatThrownBy(() -> couponStoreService.issueCoupon(
                new CouponIssueRequest(100L, OriginType.BOOK, 99L), 1L))
                .isInstanceOf(DuplicateCouponException.class);
        }
    }
}
