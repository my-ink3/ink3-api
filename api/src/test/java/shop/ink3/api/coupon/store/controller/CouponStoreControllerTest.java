package shop.ink3.api.coupon.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import shop.ink3.api.coupon.bookCoupon.entity.BookCouponRepository;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCouponRepository;
import shop.ink3.api.coupon.coupon.entity.Coupon;
import shop.ink3.api.coupon.policy.entity.DiscountType;
import shop.ink3.api.coupon.store.dto.CouponIssueRequest;
import shop.ink3.api.coupon.store.dto.CouponStoreDto;
import shop.ink3.api.coupon.store.dto.CouponStoreUpdateRequest;
import shop.ink3.api.coupon.store.dto.CouponStoreUpdateResponse;
import shop.ink3.api.coupon.store.entity.CouponStatus;
import shop.ink3.api.coupon.store.entity.CouponStore;
import shop.ink3.api.coupon.store.entity.OriginType;
import shop.ink3.api.coupon.store.service.CouponStoreService;

import java.time.LocalDateTime;
import java.util.List;
import shop.ink3.api.user.user.entity.User;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CouponStoreControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CouponStoreService couponStoreService;

    @Mock
    private BookCouponRepository bookCouponRepository;

    @Mock
    private CategoryCouponRepository categoryCouponRepository;

    private CouponStoreController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Controller에 mock 주입
        controller = new CouponStoreController(
                couponStoreService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Jackson ObjectMapper에 JavaTimeModule 등록
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    @Test
    @DisplayName("GET /users/{userId}/coupon-stores - 전체 쿠폰 조회")
    void getStoresByUserId_success() throws Exception {
        // Arrange
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(2L);
        when(mockUser.getName()).thenReturn("테스터");

        Coupon mockCoupon = mock(Coupon.class);
        when(mockCoupon.getId()).thenReturn(200L);
        when(mockCoupon.getName()).thenReturn("WELCOME5");
        when(mockCoupon.getExpiresAt()).thenReturn(LocalDateTime.of(2025, 12, 31, 0, 0));

        CouponStore dummyStore = CouponStore.builder()
                .id(2L)
                .user(mockUser)           // null이 아니어야 함
                .coupon(mockCoupon)       // null이 아니어야 함
                .originType(OriginType.WELCOME)
                .originId(null)
                .status(CouponStatus.READY)
                .issuedAt(LocalDateTime.now())
                .build();

        when(couponStoreService.getStoresByUserId(1L))
                .thenReturn(List.of(dummyStore));

        // Act & Assert
        mockMvc.perform(get("/users/1/coupon-stores")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].storeId", is(2)))
                .andExpect(jsonPath("$.data[0].userId", is(2)))
                .andExpect(jsonPath("$.data[0].userName", is("테스터")))
                .andExpect(jsonPath("$.data[0].couponId", is(200)))
                .andExpect(jsonPath("$.data[0].couponName", is("WELCOME5")))
                .andExpect(jsonPath("$.data[0].originType", is("WELCOME")))
                .andExpect(jsonPath("$.data[0].status", is("READY")));

        verify(couponStoreService, times(1)).getStoresByUserId(1L);
    }


    @Test
    @DisplayName("GET /users/{userId}/coupon-stores/status-unused - 미사용 쿠폰 조회")
    void getUnusedStoresByUserId_success() throws Exception {
        // Arrange
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(3L);
        when(mockUser.getName()).thenReturn("이영희");

        Coupon mockCoupon = mock(Coupon.class);
        when(mockCoupon.getId()).thenReturn(300L);
        when(mockCoupon.getName()).thenReturn("BIRTHDAY20");
        when(mockCoupon.getExpiresAt()).thenReturn(LocalDateTime.of(2025, 12, 31, 0, 0));

        CouponStore dummyStore = CouponStore.builder()
                .id(3L)
                .user(mockUser)       // null이 아니어야 함
                .coupon(mockCoupon)   // null이 아니어야 함
                .originType(OriginType.BIRTHDAY)
                .originId(null)
                .status(CouponStatus.READY)
                .issuedAt(LocalDateTime.now())
                .build();

        when(couponStoreService.getUnusedStoresByUserId(1L))
                .thenReturn(List.of(dummyStore));

        // Act & Assert
        mockMvc.perform(get("/users/1/coupon-stores/status-unused")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].storeId", is(3)))
                .andExpect(jsonPath("$.data[0].userId", is(3)))
                .andExpect(jsonPath("$.data[0].userName", is("이영희")))
                .andExpect(jsonPath("$.data[0].couponId", is(300)))
                .andExpect(jsonPath("$.data[0].couponName", is("BIRTHDAY20")))
                .andExpect(jsonPath("$.data[0].status", is("READY")));

        verify(couponStoreService, times(1)).getUnusedStoresByUserId(1L);
    }


    @Test
    @DisplayName("GET /coupons/{couponId}/coupon-stores - 특정 쿠폰으로 발급된 스토어 조회")
    void getStoresByCouponId_success() throws Exception {
        // Arrange
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(4L);
        when(mockUser.getName()).thenReturn("박민수");

        Coupon mockCoupon = mock(Coupon.class);
        when(mockCoupon.getId()).thenReturn(400L);
        when(mockCoupon.getName()).thenReturn("SPRING30");
        when(mockCoupon.getExpiresAt()).thenReturn(LocalDateTime.of(2025, 12, 31, 0, 0));

        CouponStore dummyStore = CouponStore.builder()
                .id(4L)
                .user(mockUser)       // null이 아니어야 함
                .coupon(mockCoupon)   // null이 아니어야 함
                .originType(OriginType.WELCOME)
                .originId(null)
                .status(CouponStatus.READY)
                .issuedAt(LocalDateTime.now())
                .build();

        when(couponStoreService.getStoresByCouponId(100L))
                .thenReturn(List.of(dummyStore));

        // Act & Assert
        mockMvc.perform(get("/coupons/100/coupon-stores")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].storeId", is(4)))
                .andExpect(jsonPath("$.data[0].userId", is(4)))
                .andExpect(jsonPath("$.data[0].userName", is("박민수")))
                .andExpect(jsonPath("$.data[0].couponId", is(400)))
                .andExpect(jsonPath("$.data[0].couponName", is("SPRING30")))
                .andExpect(jsonPath("$.data[0].originType", is("WELCOME")))
                .andExpect(jsonPath("$.data[0].status", is("READY")));

        verify(couponStoreService, times(1)).getStoresByCouponId(100L);
    }

    @Test
    @DisplayName("PUT /coupon-stores/{storeId} - 쿠폰 사용 여부 업데이트")
    void updateStore_success() throws Exception {
        // Arrange
        Long storeId = 5L;
        var updateRequest = new CouponStoreUpdateRequest(
                CouponStatus.USED,
                LocalDateTime.of(2025, 6, 4, 13, 0)
        );

        // CouponStoreUpdateResponse에 맞춰 필요한 필드만 포함하는 더미 응답 생성
        // 실제 Controller 코드: CouponStoreUpdateResponse.of(updatedStore) 형태로 생성됨
        CouponStoreUpdateResponse dummyResponse = new CouponStoreUpdateResponse(
                storeId,
                CouponStatus.USED,
                LocalDateTime.of(2025, 6, 4, 13, 0),
                String.format("CouponStore 엔트리 %d 업데이트 완료", storeId)
        );

        when(couponStoreService.updateStore(
                eq(storeId),
                ArgumentMatchers.<CouponStoreUpdateRequest>any()))
                .thenReturn(

                        CouponStore.builder()
                                .id(storeId)
                                .user(mock(User.class))
                                .coupon(mock(Coupon.class))
                                .originType(OriginType.WELCOME)
                                .originId(null)
                                .status(CouponStatus.USED)
                                .usedAt(LocalDateTime.of(2025, 6, 4, 13, 0))
                                .issuedAt(LocalDateTime.of(2025, 6, 4, 12, 0))
                                .build()
                );

        // Act & Assert
        mockMvc.perform(put("/coupon-stores/{storeId}", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                // JSON 구조: { "success": true, "data": { storeId, status, usedAt, message } }
                .andExpect(jsonPath("$.data.storeId", is(5)))
                .andExpect(jsonPath("$.data.status", is("USED")))
                // usedAt은 [2025,6,4,13,0] 배열로 나옴
                .andExpect(jsonPath("$.data.usedAt", contains(2025, 6, 4, 13, 0)))
                .andExpect(jsonPath("$.data.message", is("CouponStore 엔트리 5 업데이트 완료")));

        verify(couponStoreService, times(1))
                .updateStore(eq(storeId), ArgumentMatchers.<CouponStoreUpdateRequest>any());
    }



    @Test
    @DisplayName("DELETE /coupon-stores/{storeId} - 쿠폰 발급 삭제")
    void deleteStore_success() throws Exception {
        // Arrange
        Long storeId = 6L;
        doNothing().when(couponStoreService).deleteStore(storeId);

        // Act & Assert
        mockMvc.perform(delete("/coupon-stores/{storeId}", storeId))
                .andExpect(status().isOk())
                // CommonResponse<Void> 구조이므로 data:null / 코드에 따라 data가 아예 없을 수도 있으므로,
                // 아래와 같이 data가 null인지 또는 비어 있는지만 확인합니다.
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(couponStoreService, times(1)).deleteStore(storeId);
    }
}
