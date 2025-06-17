package shop.ink3.api.coupon.categoryCoupon.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryCouponServiceTest {

    @Mock
    private CategoryCouponRepository categoryCouponRepository;

    @InjectMocks
    private CategoryCouponService categoryCouponService;

    @Test
    @DisplayName("카테고리 ID 목록으로 CategoryCoupon fetch 조회")
    void getCategoryCouponsWithFetch_success() {
        Collection<Long> categoryIds = List.of(1L, 2L, 3L);
        List<CategoryCoupon> mockResult = Arrays.asList(
            mock(CategoryCoupon.class), mock(CategoryCoupon.class)
        );
        when(categoryCouponRepository.findAllByCategoryIdInWithFetch(categoryIds))
            .thenReturn(mockResult);

        List<CategoryCoupon> result = categoryCouponService.getCategoryCouponsWithFetch(categoryIds);

        assertThat(result).hasSize(2);
        verify(categoryCouponRepository).findAllByCategoryIdInWithFetch(categoryIds);
    }
}
