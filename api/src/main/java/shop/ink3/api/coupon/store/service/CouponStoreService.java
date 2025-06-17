package shop.ink3.api.coupon.store.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.book.bookcategory.repository.BookCategoryRepository;
import shop.ink3.api.book.category.repository.CategoryRepository;
import shop.ink3.api.book.category.service.CategoryService;
import shop.ink3.api.coupon.bookCoupon.entity.BookCouponRepository;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCoupon;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCouponService;
import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.coupon.exception.CouponNotFoundException;
import shop.ink3.api.coupon.coupon.repository.CouponRepository;
import shop.ink3.api.coupon.store.dto.CommonCouponIssueRequest;
import shop.ink3.api.coupon.store.dto.CouponIssueRequest;
import shop.ink3.api.coupon.store.dto.CouponStoreDto;
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

@RequiredArgsConstructor
@Service
@Slf4j
public class CouponStoreService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final BookCouponRepository bookCouponRepository;
    private final CategoryCouponService categoryCouponService;
    private final CouponStoreRepository couponStoreRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final CategoryService categoryService;

    /**
     * 1) 쿠폰 발급
     */
    @Transactional // write 트랜잭션
    public CouponStore issueCoupon(CouponIssueRequest req, Long userId) {
        // 1) 회원/쿠폰 존재 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Coupon policy = couponRepository.findById(req.couponId())
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found"));

        // 2) 중복 발급 검사 (originId 유·무 상관없이)
        if (req.originId() == null) {
            if (couponStoreRepository.existsByUserIdAndOriginType(user.getId(), req.originType())) {
                throw new DuplicateCouponException("Duplicate coupon found");
            }
        } else {
            if (couponStoreRepository.existsByUserIdAndCouponIdAndOriginTypeAndOriginId(
                    user.getId(), policy.getId(), req.originType(), req.originId())) {
                throw new DuplicateCouponException("Duplicate coupon found");
            }
        }

        CouponStore couponStore = CouponStore.builder()
                .user(userRepository.getReferenceById(userId))
                .coupon(couponRepository.getReferenceById(req.couponId()))
                .originType(req.originType())
                .originId(req.originId())
                .status(CouponStatus.READY)
                .usedAt(null)
                .issuedAt(LocalDateTime.now())
                .build();
        couponStoreRepository.save(couponStore);
        return couponStore;
    }

    @Transactional // write 트랜잭션
    public void issueCommonCoupon(CommonCouponIssueRequest req) {
        // 1) 회원/쿠폰 존재 검증
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new UserNotFoundException(req.userId()));
        Coupon policy = couponRepository.findById(req.couponId())
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found"));

        // 2) 중복 발급 검사 (originId 유·무 상관없이)
        if (req.originId() == null) {
            if (couponStoreRepository.existsByUserIdAndOriginType(user.getId(), req.originType())) {
                throw new DuplicateCouponException("Duplicate coupon found");
            }
        } else {
            if (couponStoreRepository.existsByUserIdAndCouponIdAndOriginTypeAndOriginId(
                    user.getId(), policy.getId(), req.originType(), req.originId())) {
                throw new DuplicateCouponException("Duplicate coupon found");
            }
        }

        CouponStore couponStore = CouponStore.builder()
                .user(userRepository.getReferenceById(req.userId()))
                .coupon(couponRepository.getReferenceById(req.couponId()))
                .originType(req.originType())
                .originId(req.originId())
                .status(CouponStatus.READY)
                .usedAt(null)
                .issuedAt(LocalDateTime.now())
                .build();
        couponStoreRepository.save(couponStore);
    }

    /**
     * 2) 유저의 모든 쿠폰 조회
     */
    @Transactional(readOnly = true)
    public List<CouponStore> getStoresByUserId(Long userId) {
        return couponStoreRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<CouponStore> getStoresPagingByUserId(Long userId, Pageable pageable) {
        return couponStoreRepository.findStoresByUserId(userId, List.of(CouponStatus.READY, CouponStatus.USED, CouponStatus.EXPIRED), pageable);
    }

    /**
     * 3) 특정 쿠폰을 가진 유저들 조회
     */
    @Transactional(readOnly = true)
    public List<CouponStore> getStoresByCouponId(Long couponId) {
        // 이 메서드가 없다면 UserCouponRepository에 추가해야 함
        return couponStoreRepository.findByCouponId(couponId);
    }

    /**
     * 4) 미사용 쿠폰만 조회
     */
    @Transactional(readOnly = true)
    public List<CouponStore> getUnusedStoresByUserId(Long userId) {
        return couponStoreRepository.findByUserIdAndStatus(userId, CouponStatus.READY);
    }

    // 미사용 쿠폰 페이징 조회
    @Transactional(readOnly = true)
    public Page<CouponStore> getUnusedStoresPagingByUserId(Long userId, Pageable pageable) {
        return couponStoreRepository.findStoresByUserId(userId, CouponStatus.READY, pageable);
    }

    // 사용 및 만료 쿠폰 페이징 조회
    @Transactional(readOnly = true)
    public Page<CouponStore> getUsedOrExpiredStoresPagingByUserId(Long userId, Pageable pageable) {
        return couponStoreRepository.findStoresByUserId(userId,
            List.of(CouponStatus.USED, CouponStatus.EXPIRED), pageable);
    }

    /**
     * 5) 사용 여부 업데이트
     */
    @Transactional
    public CouponStore updateStore(Long storeId, CouponStoreUpdateRequest req) {
        CouponStore store = couponStoreRepository.findById(storeId)
                .orElseThrow(() -> new CouponStoreNotFoundException(
                        String.format("CouponStore not found: %d", storeId)));
        store.update(req.couponStatus(), req.usedAt());
        return store; // 트랜잭션 커밋 시점에 자동으로 반영
    }

    @Transactional
    public void disableCouponStoresByCouponId(Long couponId) {
        // 1) READY 상태의 모든 스토어 조회
        List<CouponStore> stores = couponStoreRepository
                .findAllByCouponIdAndStatus(couponId, CouponStatus.READY);

        // 2) 각각 DISABLED 로 업데이트
        stores.forEach(store -> store.update(CouponStatus.DISABLED, null));

        // → 여기에 빠져 있었던 저장 호출을 추가해야 합니다.
        couponStoreRepository.saveAll(stores);
    }

    @Transactional
    public void reactivateCouponStoresByCouponId(Long couponId) {
        List<CouponStore> stores = couponStoreRepository.findAllByCouponIdAndStatus(couponId, CouponStatus.DISABLED);
        stores.forEach(store -> store.update(CouponStatus.READY, null));
        couponStoreRepository.saveAll(stores);
    }

    /**
     * 6) 삭제
     */
    @Transactional
    public void deleteStore(Long id) {
        try {
            couponStoreRepository.deleteById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new CouponStoreNotFoundException(
                    String.format("CouponStore not found: %d", id));
        }
    }

    @Transactional(readOnly = true)
    public boolean existByOriginIdAndUserId(Long userId, Long originId) {
        return couponStoreRepository.existsByOriginIdAndUserId(userId, originId);
    }


    @Transactional(readOnly = true)
    public List<CouponStoreDto> getApplicableCouponStores(Long userId, Long bookId) {
        // 1) BOOK 기반 쿠폰
        List<Long> bookCouponIds = bookCouponRepository.findIdsByBookId(bookId);
        List<CouponStore> bookStores = couponStoreRepository
                .findWithCouponByUserAndOriginAndStatus(
                        userId, OriginType.BOOK, bookCouponIds, CouponStatus.READY);

        // 2) CATEGORY 기반 쿠폰
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found: " + bookId));

        // 직접 매핑된 카테고리 ID들
        List<Long> directCategoryIds = bookCategoryRepository.findAllByBookId(bookId).stream()
                .map(bookCategory -> bookCategory.getCategory().getId())  // ✔ 진짜 카테고리 ID
                .toList();

        // 조상 카테고리 포함해서 ID 수집
        Set<Long> allCategoryIds = new HashSet<>(directCategoryIds);
        for (Long catId : directCategoryIds) {
            categoryService.getAllAncestors(catId).forEach(c -> allCategoryIds.add(c.id()));
        }

        // CategoryCoupon 엔티티를 fetch join 으로 가져오고 → ID만 뽑아 내기
        List<Long> categoryCouponIds = categoryCouponService
                .getCategoryCouponsWithFetch(allCategoryIds)
                .stream()
                .map(CategoryCoupon::getId)
                .toList();

        List<CouponStore> categoryStores = couponStoreRepository
                .findWithCouponByUserAndOriginAndStatus(
                        userId, OriginType.CATEGORY, categoryCouponIds, CouponStatus.READY);

        // 3) WELCOME 쿠폰 (coupon만 미리 fetch해서 가져옴)
        List<CouponStore> welcomeStores = couponStoreRepository
                .findWithCouponByUserAndOriginAndStatus(
                        userId, OriginType.WELCOME, CouponStatus.READY);

        // 4) BIRTHDAY 쿠폰
        List<CouponStore> birthdayStores = couponStoreRepository
                .findWithCouponByUserAndOriginAndStatus(
                        userId, OriginType.BIRTHDAY, CouponStatus.READY);

        // 5) 결합 후 기한 필터링, → 엔티티를 DTO로 매핑
        return Stream.of(bookStores, categoryStores, welcomeStores, birthdayStores)
                .flatMap(List::stream)
                .filter(cc -> cc.getCoupon().getIssuableFrom().isBefore(LocalDateTime.now())
                        || cc.getCoupon().getIssuableFrom().isEqual(LocalDateTime.now()))
                .filter(store -> store.getCoupon().getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toDto)
                .toList();
    }

    private CouponStoreDto toDto(CouponStore cs) {
        return new CouponStoreDto(
                cs.getId(),
                cs.getCoupon().getId(),
                cs.getCoupon().getName(),
                cs.getCoupon().getIssuableFrom(),
                cs.getCoupon().getExpiresAt(),
                cs.getOriginType(),
                cs.getOriginId(),
                cs.getStatus(),
                cs.getCoupon().getCouponPolicy().getDiscountType(),
                (cs.getCoupon().getCouponPolicy().getDiscountValue() != null)
                        ? cs.getCoupon().getCouponPolicy().getDiscountValue()
                        : null,
                (cs.getCoupon().getCouponPolicy().getDiscountPercentage() != null)
                        ? cs.getCoupon().getCouponPolicy().getDiscountPercentage()
                        : null,
                cs.getCoupon().getCouponPolicy().getMinimumOrderAmount(),
                cs.getCoupon().getCouponPolicy().getMaximumDiscountAmount()
        );
    }
}
