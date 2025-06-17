package shop.ink3.api.order.refund.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.service.OrderMainService;
import shop.ink3.api.order.refund.dto.RefundCreateRequest;
import shop.ink3.api.order.refund.dto.RefundResponse;
import shop.ink3.api.order.refund.dto.RefundUpdateRequest;
import shop.ink3.api.order.refund.entity.Refund;
import shop.ink3.api.order.refund.exception.RefundNotFoundException;
import shop.ink3.api.order.refund.service.RefundService;
import shop.ink3.api.user.user.entity.User;

@WebMvcTest(RefundController.class)
class RefundControllerTest {

    @MockitoBean
    RefundService refundService;

    @MockitoBean
    OrderMainService orderMainService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("반품 정보 조회 - 성공")
    void getRefund_성공() throws Exception {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        Refund refund = Refund.builder().id(1L).order(order).build();
        RefundResponse response = RefundResponse.from(refund);
        when(refundService.getOrderRefund(1L)).thenReturn(response);

        // when, then
        mockMvc.perform(get("/refunds/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
        verify(refundService, times(1)).getOrderRefund(1L);
    }

    @Test
    @DisplayName("반품 정보 조회 - 실패")
    void getRefund_실패() throws Exception {
        // given
        doThrow(new RefundNotFoundException(1L)).when(refundService).getOrderRefund(anyLong());

        // when, then
        mockMvc.perform(get("/refunds/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }


    @Test
    @DisplayName("사용자의 반품 정보 리스트 조회 - 성공")
    void getUserRefundList_성공() throws Exception {
        // given
        User user1 = User.builder().id(1L).build();
        Order order1 = Order.builder().id(1L).user(user1).build();
        User user2 = User.builder().id(1L).build();
        Order order2 = Order.builder().id(1L).user(user2).build();
        PageResponse<RefundResponse> response = new PageResponse<>(
                List.of(
                        RefundResponse.from(Refund.builder().id(1L).order(order1).reason("테스트 사유1").details("테스트 상세1").build()),
                        RefundResponse.from(Refund.builder().id(2L).order(order2).reason("테스트 사유2").details("테스트 상세2").build())
                ),
                0,2,2L, 1, false, false
        );
        when(refundService.getUserRefundList(anyLong(), any())).thenReturn(response);

        // when, then
        mockMvc.perform(get("/refunds/me")
                    .param("page", "0")
                    .param("size", "2")
                    .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].reason").value("테스트 사유1"))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.content[1].reason").value("테스트 사유2"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    @DisplayName("반품 생성 - 성공")
    void createRefund_성공() throws Exception {
        // given
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        RefundCreateRequest request = new RefundCreateRequest(1L,"테스트 사유", "테스트 상세",3000, LocalDateTime.now(),true);
        Refund refund = Refund.builder()
                .order(order)
                .reason(request.getReason())
                .details(request.getDetails())
                .createdAt(request.getCreatedAt())
                .RefundShippingFee(request.getRefundShippingFee())
                .approved(request.getApproved())
                .build();
        RefundResponse response = RefundResponse.from(refund);
        when(orderMainService.createRefund(any())).thenReturn(response);

        // when, then
        mockMvc.perform(post("/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.reason").value("테스트 사유"))
                .andDo(print());
    }

    @Test
    @DisplayName("반품 생성 - 실패")
    void createRefund_실패() throws Exception {
        // given
        RefundCreateRequest request = new RefundCreateRequest(1L,"테스트 사유", "테스트 상세", 3000, LocalDateTime.now(),true);
        doThrow(new RefundNotFoundException(1L)).when(orderMainService).createRefund(any());

        // when, then
        mockMvc.perform(post("/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }


    @Test
    @DisplayName("반품 수정 - 성공")
    void updateRefund_성공() throws Exception {
        // given
        RefundUpdateRequest request = new RefundUpdateRequest("변경 사유", "변경 상세");
        User user = User.builder().id(1L).build();
        Order order = Order.builder().id(1L).user(user).build();
        Refund refund = Refund.builder().id(1L).order(order).build();
        when(refundService.updateRefund(anyLong(),any())).thenReturn(RefundResponse.from(refund));

        // when, then
        mockMvc.perform(put("/refunds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("반품 수정 - 실패")
    void updateRefund_실패() throws Exception {
        // given
        RefundUpdateRequest request = new RefundUpdateRequest("변경 사유", "변경 상세");
        doThrow(new RefundNotFoundException(1L)).when(refundService).updateRefund(anyLong(),any());

        // when, then
        mockMvc.perform(put("/refunds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }


    @Test
    @DisplayName("반품 삭제 - 성공")
    void deleteRefund_성공() throws Exception {
        // given
        doNothing().when(refundService).deleteRefund(anyLong());

        mockMvc.perform(delete("/refunds/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("반품 삭제 - 실패")
    void deleteRefund_실패() throws Exception {
        // given
        doThrow(new RefundNotFoundException(1L)).when(refundService).deleteRefund(anyLong());

        // when, then
        mockMvc.perform(delete("/refunds/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}
