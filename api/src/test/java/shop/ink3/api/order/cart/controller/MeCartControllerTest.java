package shop.ink3.api.order.cart.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.order.cart.dto.CartRequest;
import shop.ink3.api.order.cart.dto.CartResponse;
import shop.ink3.api.order.cart.dto.CartUpdateRequest;
import shop.ink3.api.order.cart.dto.MeCartRequest;
import shop.ink3.api.order.cart.service.CartService;
import shop.ink3.api.user.user.entity.User;

@WebMvcTest(MeCartController.class)
class MeCartControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private Book book1;
    private Book book2;
    private CartRequest cartRequest;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("user").build();
        book1 = Book.builder().id(1L).title("책").originalPrice(20000).salePrice(18000).build();
        book2 = Book.builder().id(2L).title("책2").originalPrice(20000).salePrice(18000).build();

        cartRequest = new CartRequest(user.getId(), book1.getId(), 2);
        cartResponse = new CartResponse(1L, user.getId(), book1.getId(), book1.getTitle(),
            20000, 18000, 10, "url", 2);
    }

    @Test
    @DisplayName("내 장바구니 추가")
    void addCart() throws Exception {
        Mockito.when(cartService.addCartItem(any(CartRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/me/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", user.getId())
                .content(objectMapper.writeValueAsString(new MeCartRequest(book1.getId(), 2))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.bookId").value(book1.getId()));
    }

    @Test
    @DisplayName("비회원 장바구니 병합")
    void mergeGuestCart() throws Exception {
        List<MeCartRequest> guestItems = List.of(
            new MeCartRequest(book1.getId(), 2),
            new MeCartRequest(book2.getId(), 1)
        );

        mockMvc.perform(post("/me/carts/merge-guest")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", user.getId())
                .content(objectMapper.writeValueAsString(guestItems)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 장바구니 수량 수정")
    void updateQuantity() throws Exception {
        Mockito.when(cartService.updateCartQuantity(anyLong(), any(CartUpdateRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(put("/me/carts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", user.getId())
                .content(objectMapper.writeValueAsString(new CartUpdateRequest(2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @DisplayName("내 장바구니 전체 조회")
    void getCarts() throws Exception {
        Mockito.when(cartService.getCartItemsByUserId(user.getId()))
            .thenReturn(List.of(cartResponse));

        mockMvc.perform(get("/me/carts")
                .header("X-User-Id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("내 장바구니 전체 삭제")
    void deleteCarts() throws Exception {
        mockMvc.perform(delete("/me/carts")
                .header("X-User-Id", user.getId()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 장바구니 개별 삭제")
    void deleteCart() throws Exception {
        mockMvc.perform(delete("/me/carts/{cartId}", 1L)
                .header("X-User-Id", user.getId()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("내 장바구니 + 쿠폰 목록 조회")
    void getCartsWithCoupons() throws Exception {
        Mockito.when(cartService.getCartItemsWithCoupons(user.getId()))
            .thenReturn(List.of());

        mockMvc.perform(get("/me/carts/coupons")
                .header("X-User-Id", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("선택 장바구니 + 쿠폰 목록 조회")
    void getSelectCartsWithCoupons() throws Exception {
        Mockito.when(cartService.getSelectCartsWithCoupon(eq(user.getId()), any()))
            .thenReturn(List.of());

        mockMvc.perform(get("/me/carts/selected/coupons")
                .header("X-User-Id", user.getId())
                .param("cartIds", "1", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("잘못된 요청 본문 - 장바구니 추가 시 유효성 검사 실패")
    void addCart_invalidRequest() throws Exception {
        mockMvc.perform(post("/me/carts")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", user.getId())
                .content(objectMapper.writeValueAsString(new MeCartRequest(null, -1))))
            .andExpect(status().isBadRequest());
    }

}
