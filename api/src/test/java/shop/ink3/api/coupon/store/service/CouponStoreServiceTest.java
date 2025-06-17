package shop.ink3.api.coupon.store.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.coupon.exception.CouponNotFoundException;
import shop.ink3.api.coupon.coupon.repository.CouponRepository;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
import shop.ink3.api.coupon.store.dto.CommonCouponIssueRequest;
import shop.ink3.api.coupon.store.dto.CouponIssueRequest;
import shop.ink3.api.coupon.store.dto.CouponStoreUpdateRequest;
import shop.ink3.api.coupon.store.entity.CouponStatus;
import shop.ink3.api.coupon.store.entity.CouponStore;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.coupon.store.exception.CouponStoreNotFoundException;
import shop.ink3.api.coupon.store.exception.DuplicateCouponException;
import shop.ink3.api.coupon.store.repository.CouponStoreRepository;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.exception.UserNotFoundException;
import shop.ink3.api.user.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CouponStoreServiceTest {

    @InjectMocks
    private CouponStoreService couponStoreService;

    @Mock private CouponRepository couponRepository;
    @Mock private UserRepository userRepository;
    @Mock private CouponStoreRepository couponStoreRepository;

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

    @Nested
    @DisplayName("issueCommonCoupon")
    class IssueCommonCouponTest {

        @Test
        @DisplayName("공통 쿠폰 정상 발급")
        void issueCommonCoupon_success() {
            CommonCouponIssueRequest request = new CommonCouponIssueRequest(user.getId(), coupon.getId(), OriginType.WELCOME, null);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(couponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));
            when(couponStoreRepository.existsByUserIdAndOriginType(user.getId(), request.originType())).thenReturn(false);

            assertThatCode(() -> couponStoreService.issueCommonCoupon(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("중복 발급 시 예외 발생 (originId 존재)")
        void issueCommonCoupon_duplicate() {
            CommonCouponIssueRequest request = new CommonCouponIssueRequest(user.getId(), coupon.getId(), OriginType.BOOK, 999L);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(couponRepository.findById(coupon.getId())).thenReturn(Optional.of(coupon));
            when(couponStoreRepository.existsByUserIdAndCouponIdAndOriginTypeAndOriginId(
                user.getId(), coupon.getId(), OriginType.BOOK, 999L)).thenReturn(true);

            assertThatThrownBy(() -> couponStoreService.issueCommonCoupon(request))
                .isInstanceOf(DuplicateCouponException.class);
        }
    }

    @Nested
    @DisplayName("updateStore")
    class UpdateStoreTest {

        @Test
        @DisplayName("쿠폰 스토어 상태 업데이트 성공")
        void updateStore_success() {
            CouponStore store = CouponStore.builder().id(1L).status(CouponStatus.READY).build();
            CouponStoreUpdateRequest req = new CouponStoreUpdateRequest(CouponStatus.USED, LocalDateTime.now());
            when(couponStoreRepository.findById(1L)).thenReturn(Optional.of(store));

            CouponStore result = couponStoreService.updateStore(1L, req);

            assertThat(result.getStatus()).isEqualTo(CouponStatus.USED);
        }

        @Test
        @DisplayName("존재하지 않는 스토어 예외")
        void updateStore_notFound() {
            when(couponStoreRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> couponStoreService.updateStore(1L,
                new CouponStoreUpdateRequest(CouponStatus.USED, LocalDateTime.now())))
                .isInstanceOf(CouponStoreNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteStore")
    class DeleteStoreTest {

        @Test
        @DisplayName("쿠폰 스토어 삭제 성공")
        void deleteStore_success() {
            doNothing().when(couponStoreRepository).deleteById(1L);
            assertThatCode(() -> couponStoreService.deleteStore(1L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("삭제 대상이 없을 때 예외 발생")
        void deleteStore_notFound() {
            doThrow(EmptyResultDataAccessException.class).when(couponStoreRepository).deleteById(1L);
            assertThatThrownBy(() -> couponStoreService.deleteStore(1L))
                .isInstanceOf(CouponStoreNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("disable/reactivateCouponStores")
    class ToggleCouponStoreStatusTest {

        @Test
        @DisplayName("disable - 스토어가 없을 경우에도 문제없이 실행")
        void disableCouponStores_empty() {
            when(couponStoreRepository.findAllByCouponIdAndStatus(1L, CouponStatus.READY))
                .thenReturn(List.of());

            assertThatCode(() -> couponStoreService.disableCouponStoresByCouponId(1L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("reactivate - 스토어 상태 변경")
        void reactivateCouponStores_success() {
            CouponStore disabledStore = CouponStore.builder().id(1L).status(CouponStatus.DISABLED).build();
            when(couponStoreRepository.findAllByCouponIdAndStatus(1L, CouponStatus.DISABLED))
                .thenReturn(List.of(disabledStore));

            couponStoreService.reactivateCouponStoresByCouponId(1L);

            assertThat(disabledStore.getStatus()).isEqualTo(CouponStatus.READY);
            verify(couponStoreRepository).saveAll(List.of(disabledStore));
        }
    }

    @Nested
    @DisplayName("조회 관련 메서드")
    class ReadOnlyQueryTest {

        @Test
        @DisplayName("유저 ID로 쿠폰 스토어 목록 조회")
        void getStoresByUserId_success() {
            CouponStore store = CouponStore.builder().id(1L).user(user).build();
            when(couponStoreRepository.findByUserId(1L)).thenReturn(List.of(store));

            List<CouponStore> result = couponStoreService.getStoresByUserId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("유저 ID + Pageable로 쿠폰 스토어 목록 조회")
        void getStoresPagingByUserId_success() {
            CouponStore store = CouponStore.builder().id(1L).user(user).build();
            Page<CouponStore> page = new PageImpl<>(List.of(store));
            when(couponStoreRepository.findStoresByUserId(eq(1L), anyList(), any())).thenReturn(page);

            Page<CouponStore> result = couponStoreService.getStoresPagingByUserId(1L, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("쿠폰 ID로 스토어 조회")
        void getStoresByCouponId_success() {
            CouponStore store = CouponStore.builder().id(1L).coupon(coupon).build();
            when(couponStoreRepository.findByCouponId(100L)).thenReturn(List.of(store));

            List<CouponStore> result = couponStoreService.getStoresByCouponId(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCoupon().getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("미사용 쿠폰 스토어 조회")
        void getUnusedStoresByUserId_success() {
            when(couponStoreRepository.findByUserIdAndStatus(1L, CouponStatus.READY))
                .thenReturn(List.of(CouponStore.builder().id(1L).status(CouponStatus.READY).build()));

            List<CouponStore> result = couponStoreService.getUnusedStoresByUserId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(CouponStatus.READY);
        }

        @Test
        @DisplayName("미사용 쿠폰 스토어 페이징 조회")
        void getUnusedStoresPagingByUserId_success() {
            Page<CouponStore> page = new PageImpl<>(List.of(CouponStore.builder().id(1L).build()));
            when(couponStoreRepository.findStoresByUserId(eq(1L), eq(CouponStatus.READY), any()))
                .thenReturn(page);

            Page<CouponStore> result = couponStoreService.getUnusedStoresPagingByUserId(1L, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("사용/만료 쿠폰 스토어 페이징 조회")
        void getUsedOrExpiredStoresPagingByUserId_success() {
            Page<CouponStore> page = new PageImpl<>(List.of(CouponStore.builder().id(1L).build()));
            when(couponStoreRepository.findStoresByUserId(eq(1L), anyList(), any()))
                .thenReturn(page);

            Page<CouponStore> result = couponStoreService.getUsedOrExpiredStoresPagingByUserId(1L, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("originId와 userId로 쿠폰 스토어 존재 여부 확인")
        void existByOriginIdAndUserId_success() {
            when(couponStoreRepository.existsByOriginIdAndUserId(1L, 2L)).thenReturn(true);

            boolean exists = couponStoreService.existByOriginIdAndUserId(1L, 2L);

            assertThat(exists).isTrue();
        }
    }
}
