package shop.ink3.api.coupon.categoryCoupon.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryCouponService {

    private final CategoryCouponRepository categoryCouponRepository;

    /**
     * 카테고리ID로 매핑된 CategoryCoupon 목록을 조회하되,
     * 연관된 category, coupon 필드를 fetch join으로 미리 로딩한다.
     */
    @Transactional(readOnly = true)
    public List<CategoryCoupon> getCategoryCouponsWithFetch(Collection<Long> categoryIds) {
        return categoryCouponRepository.findAllByCategoryIdInWithFetch(categoryIds);
    }
}
