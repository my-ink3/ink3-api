package shop.ink3.api.coupon.coupon.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityNotFoundException;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.book.bookcategory.repository.BookCategoryRepository;
import shop.ink3.api.book.category.entity.Category;
import shop.ink3.api.book.category.repository.CategoryRepository;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.coupon.bookCoupon.entity.BookCoupon;
import shop.ink3.api.coupon.bookCoupon.entity.BookCouponRepository;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCoupon;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCouponRepository;
import shop.ink3.api.coupon.coupon.dto.CouponCreateRequest;
import shop.ink3.api.coupon.coupon.dto.CouponResponse;
import shop.ink3.api.coupon.coupon.dto.CouponResponse.BookInfo;
import shop.ink3.api.coupon.coupon.dto.CouponResponse.CategoryInfo;
import shop.ink3.api.coupon.coupon.dto.CouponUpdateRequest;
import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.coupon.exception.BusinessException;
import shop.ink3.api.coupon.coupon.exception.CouponInUseException;
import shop.ink3.api.coupon.coupon.exception.CouponNotFoundException;
import shop.ink3.api.coupon.coupon.repository.CouponRepository;
import shop.ink3.api.coupon.coupon.service.Impl.CouponServiceImpl;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
import shop.ink3.api.coupon.policy.exception.PolicyNotFoundException;
import shop.ink3.api.coupon.policy.repository.PolicyRepository;
import shop.ink3.api.coupon.store.repository.CouponStoreRepository;
import shop.ink3.api.coupon.store.service.CouponStoreService;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private BookCouponRepository bookCouponRepository;
    @Mock
    private CategoryCouponRepository categoryCouponRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookCategoryRepository bookCategoryRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CouponStoreService couponStoreService;
    @Mock
    private CouponStoreRepository couponStoreRepository;
    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    void createCoupon_success_withoutAssociations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(5);
        CouponCreateRequest req = new CouponCreateRequest(
            1L, "test", now, expires, true,
            List.of(), List.of()
        );

        when(policyRepository.findById(1L))
            .thenReturn(
                Optional.of(CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build()));

        when(couponRepository.save(any(Coupon.class)))
            .thenAnswer((Answer<Coupon>)invocation -> {
                Coupon c = invocation.getArgument(0);
                ReflectionTestUtils.setField(c, "id", 100L);
                return c;
            });

        CouponResponse resp = couponService.createCoupon(req);

        assertNotNull(resp);
        assertEquals(100L, resp.couponId());
        assertEquals("test", resp.name());
        assertEquals(1L, resp.policyId());
        assertEquals("P1", resp.policyName());
        assertEquals(now, resp.issuableFrom());
        assertEquals(expires, resp.expiresAt());
        assertTrue(resp.books().isEmpty());
        assertTrue(resp.categories().isEmpty());

        verify(policyRepository).findById(1L);
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("createCoupon - bookId 존재하지 않으면 예외")
    void createCoupon_invalidBookId_throws() {
        LocalDateTime now = LocalDateTime.now();
        CouponCreateRequest req = new CouponCreateRequest(
            1L, "invalid-book",
            now, now.plusDays(5), true,
            List.of(1000L), List.of()
        );

        when(policyRepository.findById(1L)).thenReturn(Optional.of(
            CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build()));

        when(bookRepository.findAllById(List.of(1000L))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
            () -> couponService.createCoupon(req));
    }

    @Test
    @DisplayName("createCoupon - categoryId 존재하지 않으면 예외")
    void createCoupon_invalidCategoryId_throws() {
        LocalDateTime now = LocalDateTime.now();
        CouponCreateRequest req = new CouponCreateRequest(
            1L, "invalid-category",
            now, now.plusDays(5), true,
            List.of(), List.of(9000L)
        );

        when(policyRepository.findById(1L)).thenReturn(Optional.of(
            CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build()));

        when(categoryRepository.findAllById(List.of(9000L))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
            () -> couponService.createCoupon(req));
    }

    @Test
    void createCoupon_noPolicy_throws() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        CouponCreateRequest req = new CouponCreateRequest(
            99L, "no-policy",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1), true,
            List.of(), List.of()
        );

        assertThrows(PolicyNotFoundException.class,
            () -> couponService.createCoupon(req));
        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCoupon - 만료일이 발행 시작일보다 빠르면 예외 발생")
    void createCoupon_invalidDate_throws() {
        LocalDateTime now = LocalDateTime.now();
        CouponCreateRequest req = new CouponCreateRequest(
            1L, "invalid-date",
            now.plusDays(2),
            now,
            true,
            List.of(), List.of()
        );

        assertThrows(BusinessException.class, () -> couponService.createCoupon(req));
    }

    @Test
    void getCouponById_success() {
        CouponPolicy policy = CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build();
        Coupon coupon = Coupon.builder()
            .id(7L)
            .couponPolicy(policy)
            .name("test")
            .issuableFrom(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusDays(1))
            .createdAt(LocalDateTime.now())
            .build();

        when(couponRepository.findByIdWithFetch(7L))
            .thenReturn(Optional.of(coupon));

        CouponResponse resp = couponService.getCouponById(7L);

        assertEquals(7L, resp.couponId());
        assertEquals("test", resp.name());
        assertEquals(1L, resp.policyId());
        assertEquals("P1", resp.policyName());
    }

    @Test
    void getCouponById_notFound_throws() {
        when(couponRepository.findByIdWithFetch(5L))
            .thenReturn(Optional.empty());

        assertThrows(CouponNotFoundException.class,
            () -> couponService.getCouponById(5L));
    }

    @Test
    void getAllCoupons_success() {
        CouponPolicy policy = CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build();
        Coupon c1 = Coupon.builder().id(11L).couponPolicy(policy).build();
        Coupon c2 = Coupon.builder().id(22L).couponPolicy(policy).build();

        Pageable unpaged = Pageable.unpaged();
        when(couponRepository.findAllWithAssociations(unpaged))
            .thenReturn(new PageImpl<>(List.of(c1, c2)));

        PageResponse<CouponResponse> page = couponService.getAllCoupons(unpaged);
        List<CouponResponse> list = page.content();

        assertEquals(2, list.size());
        assertEquals(11L, list.get(0).couponId());
        assertEquals(22L, list.get(1).couponId());
    }

    @Test
    void getCouponsByBookId_success() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime issuableFrom = now.minusDays(1);
        LocalDateTime expires = now.plusDays(5);
        BookCoupon bc = mock(BookCoupon.class);
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(10L);
        when(book.getTitle()).thenReturn("Java");

        CouponPolicy policy = CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build();
        Coupon coupon = Coupon.builder()
            .id(99L)
            .couponPolicy(policy)
            .name("B1")
            .issuableFrom(issuableFrom)
            .expiresAt(expires)
            .isActive(true)
            .build();
        when(bc.getBook()).thenReturn(book);
        when(bc.getCoupon()).thenReturn(coupon);
        when(bc.getId()).thenReturn(5L);

        Pageable unpaged = Pageable.unpaged();
        when(bookCouponRepository.findAllByBookId(10L, unpaged))
            .thenReturn(new PageImpl<>(List.of(bc)));

        PageResponse<CouponResponse> page =
            couponService.getCouponsByBookId(10L, unpaged);
        CouponResponse resp = page.content().getFirst();
        BookInfo info = resp.books().getFirst();

        assertEquals(5L, info.originId());
        assertEquals(10L, info.id());
        assertEquals("Java", info.title());
        assertEquals("BOOK", info.originType());
    }

    @Test
    void getCouponsByBookId_empty_throws() {
        Pageable unpaged = Pageable.unpaged();
        when(bookCouponRepository.findAllByBookId(123L, unpaged))
            .thenReturn(new PageImpl<>(List.of()));

        assertThrows(CouponNotFoundException.class,
            () -> couponService.getCouponsByBookId(123L, unpaged));
    }

    @Test
    @DisplayName("getCouponsByBookId - inactive 쿠폰만 있을 때 예외 발생")
    void getCouponsByBookId_onlyInactiveCoupons_throws() {
        Book book = mock(Book.class);

        Coupon coupon = Coupon.builder()
            .id(1L)
            .name("inactive")
            .issuableFrom(LocalDateTime.now().minusDays(1))
            .expiresAt(LocalDateTime.now().plusDays(1))
            .isActive(false)
            .build();

        BookCoupon bc = mock(BookCoupon.class);
        when(bc.getCoupon()).thenReturn(coupon);

        when(bookCouponRepository.findAllByBookId(eq(10L), any()))
            .thenReturn(new PageImpl<>(List.of(bc)));

        assertThrows(CouponInUseException.class,
            () -> couponService.getCouponsByBookId(10L, Pageable.unpaged()));
    }

    @Test
    void getCouponsByCategoryId_success() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(5);
        LocalDateTime issuableFrom = now.minusDays(1);
        CategoryCoupon cc = mock(CategoryCoupon.class);
        Category cat = mock(Category.class);
        when(cat.getId()).thenReturn(8L);
        when(cat.getName()).thenReturn("Fiction");

        CouponPolicy policy = CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build();
        Coupon coupon = Coupon.builder()
            .id(55L)
            .couponPolicy(policy)
            .name("C1")
            .issuableFrom(issuableFrom)
            .expiresAt(expires)
            .isActive(true)
            .build();

        when(cc.getCategory()).thenReturn(cat);
        when(cc.getCoupon()).thenReturn(coupon);
        when(cc.getId()).thenReturn(6L);

        Pageable unpaged = Pageable.unpaged();
        when(categoryCouponRepository.findAllByCategoryId(8L, unpaged))
            .thenReturn(new PageImpl<>(List.of(cc)));

        PageResponse<CouponResponse> page =
            couponService.getCouponsByCategoryId(8L, unpaged);
        CategoryInfo info = page.content().getFirst().categories().getFirst();

        assertEquals(6L, info.originId());
        assertEquals(8L, info.id());
        assertEquals("Fiction", info.name());
        assertEquals("CATEGORY", info.originType());
    }

    @Test
    void getCouponsByCategoryId_empty_throws() {
        Pageable unpaged = Pageable.unpaged();
        when(categoryCouponRepository.findAllByCategoryId(7L, unpaged))
            .thenReturn(new PageImpl<>(List.of()));

        assertThrows(CouponNotFoundException.class,
            () -> couponService.getCouponsByCategoryId(7L, unpaged));
    }

    @Test
    @DisplayName("getCouponsByCategoryId - inactive 쿠폰만 있을 때 예외 발생")
    void getCouponsByCategoryId_onlyInactiveCoupons_throws() {
        Category category = mock(Category.class);

        Coupon coupon = Coupon.builder()
            .id(2L)
            .name("inactive")
            .issuableFrom(LocalDateTime.now().minusDays(1))
            .expiresAt(LocalDateTime.now().plusDays(1))
            .isActive(false)
            .build();

        CategoryCoupon cc = mock(CategoryCoupon.class);
        when(cc.getCoupon()).thenReturn(coupon);

        when(categoryCouponRepository.findAllByCategoryId(eq(1L), any()))
            .thenReturn(new PageImpl<>(List.of(cc)));

        assertThrows(CouponInUseException.class,
            () -> couponService.getCouponsByCategoryId(1L, Pageable.unpaged()));
    }

    @Test
    @DisplayName("getCouponByParentId - 도서가 존재하지 않으면 예외")
    void getCouponsByParentId_bookNotFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
            () -> couponService.getCouponsByParentId(1L, Pageable.unpaged()));
    }

    @Test
    @DisplayName("getCouponByParentId - 유효한 쿠폰이 없으면 예외")
    void getCouponsByParentId_noValidCoupons() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookCategoryRepository.findAllByBookId(1L)).thenReturn(List.of());
        when(categoryCouponRepository.findAllByCategoryIdInWithFetch(any())).thenReturn(List.of());

        assertThrows(CouponNotFoundException.class,
            () -> couponService.getCouponsByParentId(1L, Pageable.unpaged()));
    }

    @Test
    @DisplayName("updateCoupon - 성공 케이스")
    void updateCoupon_success() {
        Long couponId = 5L;
        LocalDateTime now = LocalDateTime.of(2025, 5, 31, 0, 0);
        LocalDateTime newExpires = now.plusDays(10);

        Coupon existing = Coupon.builder()
            .id(couponId)
            .couponPolicy(CouponPolicy.builder().id(1L).name("P1").discountPercentage(10).discountValue(0).build())
            .name("old-name")
            .issuableFrom(now)
            .expiresAt(now.plusDays(5))
            .createdAt(now)
            .bookCoupons(new HashSet<>())
            .categoryCoupons(new HashSet<>())
            .build();

        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.of(existing));

        when(policyRepository.findById(2L))
            .thenReturn(
                Optional.of(CouponPolicy.builder().id(2L).name("P2").discountPercentage(10).discountValue(0).build()));

        List<Long> bookIds = List.of(101L, 102L);
        List<Long> catIds = List.of(201L);

        Book b1 = mock(Book.class);
        when(b1.getId()).thenReturn(101L);
        when(b1.getTitle()).thenReturn("Java Programming");

        Book b2 = mock(Book.class);
        when(b2.getId()).thenReturn(102L);
        when(b2.getTitle()).thenReturn("Spring Boot");

        when(bookRepository.findAllById(bookIds))
            .thenReturn(List.of(b1, b2));

        Category cat = mock(Category.class);
        when(cat.getId()).thenReturn(201L);
        when(cat.getName()).thenReturn("Fiction");
        when(categoryRepository.findAllById(catIds))
            .thenReturn(List.of(cat));

        when(couponRepository.save(existing)).thenReturn(existing);

        CouponUpdateRequest req = new CouponUpdateRequest(
            2L, "new-name", now, newExpires, true,
            bookIds, catIds
        );

        CouponResponse resp = couponService.updateCoupon(couponId, req);

        assertEquals(couponId, resp.couponId());
        assertEquals("new-name", resp.name());
        assertEquals(2L, resp.policyId());
        assertEquals("P2", resp.policyName());
        assertEquals(now, resp.issuableFrom());
        assertEquals(newExpires, resp.expiresAt());

        assertTrue(resp.books().stream()
            .anyMatch(bi -> bi.id().equals(101L) && bi.title().equals("Java Programming")));
        assertTrue(resp.books().stream()
            .anyMatch(bi -> bi.id().equals(102L) && bi.title().equals("Spring Boot")));

        assertEquals(1, resp.categories().size());
        assertEquals(201L, resp.categories().getFirst().id());
        assertEquals("Fiction", resp.categories().getFirst().name());
    }

    @Test
    @DisplayName("updateCoupon — 비활성 상태에서 활성으로 변경 시 reactivation 호출")
    void updateCoupon_whenBecomesActive_thenReactivateStores() {
        Long couponId = 10L;
        LocalDateTime now = LocalDateTime.now();

        Coupon existing = Coupon.builder()
            .id(couponId)
            .isActive(false)
            .couponPolicy(CouponPolicy.builder()
                .id(2L)
                .name("test-policy")
                .discountPercentage(10)
                .discountValue(1000)
                .build())
            .build();
        when(couponRepository.findByIdWithFetch(couponId)).thenReturn(Optional.of(existing));
        when(policyRepository.findById(2L)).thenReturn(Optional.of(CouponPolicy.builder()
            .id(2L)
            .name("test-policy")
            .discountPercentage(10)
            .discountValue(1000)
            .build()));

        Coupon updated = Coupon.builder()
            .id(couponId)
            .isActive(true)
            .couponPolicy(CouponPolicy.builder()
                .id(2L)
                .name("test-policy")
                .discountPercentage(10)
                .discountValue(1000)
                .build())
            .build();
        when(couponRepository.save(any())).thenReturn(updated);

        CouponUpdateRequest req = new CouponUpdateRequest(2L, "updated", now, now.plusDays(1), true, List.of(),
            List.of());

        couponService.updateCoupon(couponId, req);

        verify(couponStoreService).reactivateCouponStoresByCouponId(couponId);
    }

    @Test
    @DisplayName("updateCoupon - CouponNotFoundException 발생")
    void updateCoupon_notFound() {
        Long couponId = 99L;
        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.empty());

        CouponUpdateRequest req = new CouponUpdateRequest(
            1L, "irrelevant",
            LocalDateTime.now(), LocalDateTime.now().plusDays(1), true,
            Collections.emptyList(), Collections.emptyList()
        );

        assertThrows(CouponNotFoundException.class,
            () -> couponService.updateCoupon(couponId, req));
    }

    @Test
    @DisplayName("updateCoupon - PolicyNotFoundException 발생")
    void updateCoupon_policyNotFound() {
        Long couponId = 7L;
        LocalDateTime now = LocalDateTime.now();
        Coupon existing = Coupon.builder()
            .id(couponId)
            .couponPolicy(CouponPolicy.builder().id(1L).build())
            .build();

        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.of(existing));
        when(policyRepository.findById(2L)).thenReturn(Optional.empty());

        CouponUpdateRequest req = new CouponUpdateRequest(
            2L, "name", now, now.plusDays(2), true,
            Collections.emptyList(), Collections.emptyList()
        );

        assertThrows(PolicyNotFoundException.class,
            () -> couponService.updateCoupon(couponId, req));
    }

    @Test
    @DisplayName("updateCoupon - invalid Book ID 예외")
    void updateCoupon_invalidBookId() {
        Long couponId = 3L;
        LocalDateTime now = LocalDateTime.now();
        Coupon existing = Coupon.builder()
            .id(couponId)
            .couponPolicy(CouponPolicy.builder().id(1L).build())
            .build();

        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.of(existing));
        when(policyRepository.findById(1L))
            .thenReturn(Optional.of(CouponPolicy.builder().id(1L).build()));
        List<Long> badBooks = List.of(999L);
        when(bookRepository.findAllById(badBooks)).thenReturn(Collections.emptyList());

        CouponUpdateRequest req = new CouponUpdateRequest(
            1L, "name", now, now.plusDays(2), true,
            badBooks, Collections.emptyList()
        );

        assertThrows(IllegalArgumentException.class,
            () -> couponService.updateCoupon(couponId, req));
    }

    @Test
    @DisplayName("updateCoupon - invalid Category ID 예외")
    void updateCoupon_invalidCategoryId() {
        Long couponId = 4L;
        LocalDateTime now = LocalDateTime.now();
        Coupon existing = Coupon.builder()
            .id(couponId)
            .couponPolicy(CouponPolicy.builder().id(1L).build())
            .build();

        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.of(existing));
        when(policyRepository.findById(1L))
            .thenReturn(Optional.of(CouponPolicy.builder().id(1L).build()));

        List<Long> badCats = List.of(888L);
        when(categoryRepository.findAllById(badCats)).thenReturn(Collections.emptyList());

        CouponUpdateRequest req = new CouponUpdateRequest(
            1L, "name", now, now.plusDays(2), true,
            Collections.emptyList(), badCats
        );

        assertThrows(IllegalArgumentException.class,
            () -> couponService.updateCoupon(couponId, req));
    }

    @Test
    @DisplayName("updateCoupon — isActive=false 로 변경되면 CouponStoreService.disable 호출")
    void updateCoupon_whenBecomesInactive_thenDisableStores() {

        // ───── given ─────
        Long couponId = 5L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpires = now.plusDays(10);

        /* ① 수정 전 쿠폰(활성) */
        Coupon before = Coupon.builder()
            .id(couponId)
            .couponPolicy(CouponPolicy.builder().id(1L).build())
            .name("old")
            .issuableFrom(now)
            .expiresAt(now.plusDays(5))
            .isActive(true)
            .build();
        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.of(before));

        /* ② 새 정책 */
        CouponPolicy newPolicy = CouponPolicy.builder()
            .id(2L).discountPercentage(10).discountValue(0)
            .build();
        when(policyRepository.findById(2L)).thenReturn(Optional.of(newPolicy));

        /* ③ 저장 이후 상태(비활성) 객체를 직접 build */
        Coupon after = Coupon.builder()
            .id(couponId)
            .couponPolicy(newPolicy)     // 정책이 바뀐 경우
            .name("new")
            .issuableFrom(now)
            .expiresAt(newExpires)
            .isActive(false)             // ← 비활성!
            .build();
        when(couponRepository.save(any(Coupon.class))).thenReturn(after);

        /* ④ 요청(active=false) */
        CouponUpdateRequest req = new CouponUpdateRequest(
            2L, "new", now, newExpires,
            false,                       // 비활성로 변경
            List.of(), List.of()
        );

        // ───── when ─────
        couponService.updateCoupon(couponId, req);

        // ───── then ─────
        verify(couponStoreService, times(1))
            .disableCouponStoresByCouponId(couponId);
    }

    @Test
    @DisplayName("updateCoupon — isActive=true 일 때 CouponStoreService.disable 호출 안 됨")
    void updateCoupon_whenStaysActive_thenDoNotDisableStores() {
        // given
        Long couponId = 5L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpires = now.plusDays(10);

        // 기존 쿠폰 엔티티 준비 (isActive=true)
        Coupon existingCoupon = Coupon.builder()
            .id(couponId)
            .couponPolicy(CouponPolicy.builder().id(1L).build())
            .name("Active Coupon")
            .issuableFrom(now)
            .expiresAt(now.plusDays(5))
            .isActive(true)
            .build();

        when(couponRepository.findByIdWithFetch(couponId))
            .thenReturn(Optional.of(existingCoupon));

        // 새 정책 준비
        CouponPolicy newPolicy = CouponPolicy.builder().id(2L).discountPercentage(10).discountValue(0).build();
        when(policyRepository.findById(2L))
            .thenReturn(Optional.of(newPolicy));

        // couponRepository.save(coupon)이 수정된 쿠폰을 그대로 반환하도록 설정
        // 이 쿠폰의 isActive()는 true를 반환할 것임
        when(couponRepository.save(any(Coupon.class))).thenReturn(existingCoupon);

        // 활성 상태를 유지하는 요청
        CouponUpdateRequest req = new CouponUpdateRequest(
            2L, "Updated Active Coupon", now, newExpires,
            true,            // isActive=true!
            List.of(), List.of()
        );

        // when
        couponService.updateCoupon(couponId, req);

        // then
        // disableCouponStoresByCouponId가 호출되지 않아야 함
        verify(couponStoreService, never())
            .disableCouponStoresByCouponId(anyLong());
    }

    @Test
    @DisplayName("deleteCouponById: when no stores issued, should delete coupon")
    void deleteCouponById_whenNotIssued_delegatesToRepository() {
        // given
        long couponId = 42L;
        when(couponStoreRepository.existsByCouponId(couponId)).thenReturn(false);

        // when
        couponService.deleteCouponById(couponId);

        // then
        verify(couponRepository, times(1)).deleteById(couponId);
    }

    @Test
    @DisplayName("deleteCouponById: when stores already issued, should throw and not delete")
    void deleteCouponById_whenIssued_throwsAndDoesNotDelete() {
        // given
        long couponId = 99L;
        when(couponStoreRepository.existsByCouponId(couponId)).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> couponService.deleteCouponById(couponId))
            .isInstanceOf(CouponInUseException.class)
            .hasMessageContaining("이미 사용자에게 발급된 쿠폰");

        verify(couponRepository, never()).deleteById(anyLong());
    }
}
