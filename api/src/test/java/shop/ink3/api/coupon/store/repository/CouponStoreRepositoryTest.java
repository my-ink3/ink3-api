package shop.ink3.api.coupon.store.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.entity.BookStatus;
import shop.ink3.api.book.publisher.entity.Publisher;
import shop.ink3.api.coupon.bookCoupon.entity.BookCoupon;
import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.store.entity.CouponStatus;
import shop.ink3.api.coupon.store.entity.CouponStore;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.user.membership.entity.Membership;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.entity.UserStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class CouponStoreRepositoryTest {

    @Autowired
    private CouponStoreRepository couponStoreRepository;

    @Autowired
    private TestEntityManager em;


    @Test
    @DisplayName("originId, userId에 매핑된 CouponStore가 있으면 true를 반환한다")
    void existByOriginIdAndUserId_returnsTrue_whenExists() {
        // given: User, Coupon, CouponStore 세팅
        Membership membership = Membership.builder()
                // 필수 non-null 필드들
                .name("Basic")                // 예시 이름
                .conditionAmount(0)           // 기본 조건 금액
                .pointRate(0)                 // 기본 포인트 적립률
                .isActive(true)               // 활성화 여부 (기본값 false 이므로 필요시 true 로)
                .isDefault(true)              // 기본 멤버십 여부
                .createdAt(LocalDateTime.now()) // 생성 시각
                .build();

// JPA가 IDENTITY 전략으로 id를 채운 뒤 반환합니다.
        membership = em.persistAndFlush(membership);
        User user = em.persistAndFlush(
                User.builder()
                        .loginId("tester1")
                        .password("pass1234")
                        .name("테스터")
                        .email("tester1@example.com")
                        .phone("010-1234-5678")
                        .birthday(LocalDate.of(1990, 1, 1))
                        .createdAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .membership(membership)
                        .status(UserStatus.ACTIVE)
                        .point(0)
                        .build()
        );

        // Coupon 생성 (필수 필드 채우기)
        Coupon coupon = em.persistAndFlush(
                Coupon.builder()
                        .name("TEST_COUPON")
                        .isActive(true)
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .build()
        );
        // 1) Publisher 준비
        Publisher publisher = em.persistAndFlush(
                Publisher.builder()
                        .name("Test Publisher")
                        .build()
        );
    }

    @Test
    @DisplayName("매칭되는 CouponStore가 없으면 false를 반환한다")
    void existByOriginIdAndUserId_returnsFalse_whenNotExists() {
        // given: Membership 세팅
        Membership membership = em.persistAndFlush(
                Membership.builder()
                        .name("Basic")
                        .conditionAmount(0)
                        .pointRate(0)
                        .isActive(true)
                        .isDefault(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // given: User 세팅 (CouponStore는 저장하지 않음)
        User user = em.persistAndFlush(
                User.builder()
                        .loginId("tester2")
                        .password("pass1234")
                        .name("테스터2")
                        .email("tester2@example.com")
                        .phone("010-9876-5432")
                        .birthday(LocalDate.of(1991, 2, 2))
                        .createdAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .membership(membership)
                        .status(UserStatus.ACTIVE)
                        .point(0)
                        .build()
        );

        // when
        boolean exists = couponStoreRepository.existsByOriginIdAndUserId(999L, user.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("status, userId, originType에 매핑된 CouponStore가 있으면 true를 반환한다")
    void existsByStatusAndUserIdAndOriginType_returnsTrue_whenExists() {
        // given: Membership, User, Coupon 저장
        Membership membership = em.persistAndFlush(
                Membership.builder()
                        .name("Basic")
                        .conditionAmount(0)
                        .pointRate(0)
                        .isActive(true)
                        .isDefault(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        User user = em.persistAndFlush(
                User.builder()
                        .loginId("tester3")
                        .password("pass1234")
                        .name("테스터3")
                        .email("tester3@example.com")
                        .phone("010-0000-0000")
                        .birthday(LocalDate.of(1992, 3, 3))
                        .createdAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .membership(membership)
                        .status(UserStatus.ACTIVE)
                        .point(0)
                        .build()
        );

        Coupon coupon = em.persistAndFlush(
                Coupon.builder()
                        .name("STATUS_TEST_COUPON")
                        .expiresAt(LocalDateTime.now().plusDays(7))
                        .build()
        );

        // and given: CouponStore(status=READY, originType=BIRTHDAY)
        em.persistAndFlush(
                CouponStore.builder()
                        .user(user)
                        .coupon(coupon)
                        .status(CouponStatus.READY)
                        .originType(OriginType.BIRTHDAY)
                        .issuedAt(LocalDateTime.now())
                        .build()
        );

        // when
        boolean exists = couponStoreRepository
                .existsByStatusAndUserIdAndOriginType(
                        CouponStatus.READY,
                        user.getId(),
                        OriginType.BIRTHDAY
                );

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("status, userId, originType에 매핑된 CouponStore가 없으면 false를 반환한다")
    void existsByStatusAndUserIdAndOriginType_returnsFalse_whenNotExists() {
        // given: Membership, User만 저장 (CouponStore는 저장하지 않음)
        Membership membership = em.persistAndFlush(
                Membership.builder()
                        .name("Basic")
                        .conditionAmount(0)
                        .pointRate(0)
                        .isActive(true)
                        .isDefault(true)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        User user = em.persistAndFlush(
                User.builder()
                        .loginId("tester4")
                        .password("pass1234")
                        .name("테스터4")
                        .email("tester4@example.com")
                        .phone("010-1111-1111")
                        .birthday(LocalDate.of(1993, 4, 4))
                        .createdAt(LocalDateTime.now())
                        .lastLoginAt(LocalDateTime.now())
                        .membership(membership)
                        .status(UserStatus.ACTIVE)
                        .point(0)
                        .build()
        );

        // when
        boolean exists = couponStoreRepository
                .existsByStatusAndUserIdAndOriginType(
                        CouponStatus.USED,
                        user.getId(),
                        OriginType.WELCOME
                );

        // then
        assertThat(exists).isFalse();
    }
}
