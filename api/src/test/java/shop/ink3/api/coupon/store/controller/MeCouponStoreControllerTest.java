package shop.ink3.api.coupon.store.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
import shop.ink3.api.coupon.policy.entity.DiscountType;
import shop.ink3.api.coupon.store.dto.CouponIssueRequest;
import shop.ink3.api.coupon.store.dto.CouponStoreDto;
import shop.ink3.api.coupon.store.dto.CouponStoreUpdateRequest;
import shop.ink3.api.coupon.store.entity.*;
import shop.ink3.api.coupon.store.service.CouponStoreService;
import shop.ink3.api.user.user.entity.User;

@WebMvcTest(MeCouponStoreController.class)
class MeCouponStoreControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CouponStoreService couponStoreService;

    @Autowired
    ObjectMapper objectMapper;

    private User user;
    private Coupon coupon;
    private CouponPolicy couponPolicy;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .id(1L)
            .name("테스트 유저")
            .email("test@example.com")
            .build();

        couponPolicy = CouponPolicy.builder()
            .id(1L)
            .discountValue(2000) // NPE 방지용 필수 필드
            .discountType(DiscountType.FIXED)
            .build();

        coupon = Coupon.builder()
            .id(1L)
            .name("테스트 쿠폰")
            .couponPolicy(couponPolicy)
            .build();
    }

    @Test
    @DisplayName("쿠폰 발급 성공")
    void issueCouponTest() throws Exception {
        Long userId = user.getId();

        CouponStore store = CouponStore.builder()
            .id(1L)
            .user(user)
            .coupon(coupon)
            .originType(OriginType.BIRTHDAY)
            .status(CouponStatus.READY)
            .build();

        given(couponStoreService.issueCoupon(any(), eq(userId))).willReturn(store);

        mockMvc.perform(MockMvcRequestBuilders.post("/me/users/coupon-stores")
                .header("X-User-Id", userId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CouponIssueRequest(1L, OriginType.BIRTHDAY, null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.storeId").value(1));
    }

    @Test
    @DisplayName("적용 가능한 쿠폰 조회 API 테스트")
    void getApplicableCouponsTest() throws Exception {
        Long userId = user.getId();
        Long bookId = 100L;

        List<CouponStoreDto> mockResult = List.of(
            new CouponStoreDto(1L, 1L, "쿠폰A", LocalDateTime.now(), LocalDateTime.now(), OriginType.BIRTHDAY, 1L,
                CouponStatus.READY, DiscountType.FIXED, 1000, 0, 1, 1),
            new CouponStoreDto(2L, 2L, "쿠폰B", LocalDateTime.now(), LocalDateTime.now(), OriginType.BIRTHDAY, 2L,
                CouponStatus.READY, DiscountType.FIXED, 1000, 0, 1, 1)
        );

        given(couponStoreService.getApplicableCouponStores(userId, bookId)).willReturn(mockResult);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/applicable-coupons")
                .param("userId", userId.toString())
                .param("bookId", bookId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].storeId").value(1))
            .andExpect(jsonPath("$[1].discountValue").value(1000));
    }

    @Test
    @DisplayName("사용자 미사용 쿠폰 목록 조회")
    void getUnusedStoresTest() throws Exception {
        given(couponStoreService.getUnusedStoresPagingByUserId(eq(user.getId()), any()))
            .willReturn(new PageImpl<>(List.of(CouponStore.builder().id(1L).user(user).coupon(coupon).build())));

        mockMvc.perform(MockMvcRequestBuilders.get("/me/users/coupon-stores/status-unused")
                .header("X-User-Id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].storeId").value(1));
    }

    @Test
    @DisplayName("사용자 사용/만료 쿠폰 목록 조회")
    void getUsedExpiredStoresTest() throws Exception {
        given(couponStoreService.getUsedOrExpiredStoresPagingByUserId(eq(user.getId()), any()))
            .willReturn(new PageImpl<>(List.of(CouponStore.builder().id(2L).user(user).coupon(coupon).build())));

        mockMvc.perform(MockMvcRequestBuilders.get("/me/users/coupon-stores/status-used")
                .header("X-User-Id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].storeId").value(2));
    }

    @Test
    @DisplayName("특정 쿠폰 ID로 발급된 쿠폰스토어 목록 조회")
    void getStoresByCouponIdTest() throws Exception {
        given(couponStoreService.getStoresByCouponId(1L))
            .willReturn(List.of(CouponStore.builder().id(3L).coupon(coupon).user(user).build()));

        mockMvc.perform(MockMvcRequestBuilders.get("/me/coupons/1/coupon-stores"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].storeId").value(3));
    }


    @Test
    @DisplayName("사용자의 쿠폰 목록 페이징 조회")
    void getStoresByUserIdTest() throws Exception {
        Long userId = this.user.getId();

        CouponStore store = CouponStore.builder()
            .id(1L)
            .user(user)
            .coupon(coupon)
            .build();

        Page<CouponStore> mockPage = new PageImpl<>(List.of(store), PageRequest.of(0, 10), 1);
        given(couponStoreService.getStoresPagingByUserId(eq(userId), any()))
            .willReturn(mockPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/users/coupon-stores")
                .header("X-User-Id", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("쿠폰 사용 여부 업데이트")
    void updateStoreTest() throws Exception {
        given(couponStoreService.updateStore(eq(1L), any()))
            .willReturn(CouponStore.builder().id(1L).user(user).coupon(coupon).status(CouponStatus.USED).build());

        mockMvc.perform(MockMvcRequestBuilders.put("/me/coupon-stores/1")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(new CouponStoreUpdateRequest(CouponStatus.USED, LocalDateTime.now()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.storeId").value(1));
    }

    @Test
    @DisplayName("쿠폰 발급 삭제")
    void deleteStoreTest() throws Exception {
        willDoNothing().given(couponStoreService).deleteStore(1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/me/coupon-stores/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
