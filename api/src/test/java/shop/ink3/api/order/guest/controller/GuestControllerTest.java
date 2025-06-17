package shop.ink3.api.order.guest.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.order.guest.dto.GuestResponse;
import shop.ink3.api.order.guest.exception.GuestOrderNotFoundException;
import shop.ink3.api.order.guest.service.GuestService;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuestController.class)
class GuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestService guestService;

    @Test
    @DisplayName("비회원 조회 성공")
    void getGuest_success() throws Exception {
        long orderId = 1L;
        GuestResponse response = new GuestResponse(1L, orderId, "guest@email.com");

        when(guestService.getGuestByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/guests")
                        .param("orderId", String.valueOf(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guestId").value(response.guestId()))
                .andExpect(jsonPath("$.data.orderId").value(response.orderId()))
                .andExpect(jsonPath("$.data.email").value(response.email()));
    }

    @Test
    @DisplayName("비회원 조회 실패 - 존재하지 않음")
    void getGuest_notFound() throws Exception {
        long orderId = 1L;

        when(guestService.getGuestByOrderId(orderId)).thenThrow(new GuestOrderNotFoundException());

        mockMvc.perform(get("/guests")
                        .param("orderId", String.valueOf(orderId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("비회원 삭제 성공")
    void deleteGuest_success() throws Exception {
        long orderId = 1L;

        doNothing().when(guestService).deleteGuestOrderByOrderId(orderId);

        mockMvc.perform(delete("/guests/orders/{orderId}", orderId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("비회원 삭제 실패 - 존재하지 않음")
    void deleteGuest_notFound() throws Exception {
        long orderId = 1L;
        doThrow(new GuestOrderNotFoundException())
                .when(guestService).deleteGuestOrderByOrderId(orderId);
        mockMvc.perform(delete("/guests/orders/{orderId}", orderId))
                .andExpect(status().isNotFound());
    }
}
