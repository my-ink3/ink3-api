package shop.ink3.api.order.shipment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import shop.ink3.api.order.shipment.dto.ShipmentResponse;
import shop.ink3.api.order.shipment.dto.ShipmentUpdateRequest;
import shop.ink3.api.order.shipment.entity.Shipment;
import shop.ink3.api.order.shipment.exception.ShipmentNotFoundException;
import shop.ink3.api.order.shipment.service.ShipmentService;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @MockitoBean
    ShipmentService shipmentService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("배송 정보 조회 - 성공")
    void getShipment() throws Exception {
        // given
        Order order = Order.builder().id(1L).build();
        Shipment shipment = Shipment.builder().id(1L).order(order).build();
        ShipmentResponse response = ShipmentResponse.from(shipment);
        when(shipmentService.getShipment(anyLong())).thenReturn(response);

        // when, then
        mockMvc.perform(get("/shipments/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }
    @Test
    @DisplayName("배송 정보 조회 - 실패")
    void getShipment_실패() throws Exception {
        // given
        when(shipmentService.getShipment(1L)).thenThrow(new ShipmentNotFoundException(1L));

        // when, then
        mockMvc.perform(get("/shipments/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("주문 상태별 배송 리스트 조회 - 성공")
    void getUserShipmentByOrderStatus_성공() throws Exception {
        // given
        Order order1 = Order.builder().id(1L).build();
        Order order2 = Order.builder().id(2L).build();
        PageResponse<ShipmentResponse> response = new PageResponse<>(
                List.of(
                        ShipmentResponse.from(Shipment.builder().id(1L).order(order1).build()),
                        ShipmentResponse.from(Shipment.builder().id(2L).order(order2).build())
                ),
                0, 2, 2L, 1, false, false
        );
        when(shipmentService.getShipmentListByOrderStatus(anyLong(), any(), any())).thenReturn(response);

        // when, then
        mockMvc.perform(get("/shipments/me/order-status")
                        .param("orderStatus", "SHIPPING")
                        .param("page", "0")
                        .param("size", "2")
                        .header("X-User-Id",  1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[1].id").value(2L))
                .andDo(print());
    }


    @Test
    @DisplayName("배송 수정 - 성공")
    void updateShipment_성공() throws Exception {
        // given
        ShipmentUpdateRequest request = new ShipmentUpdateRequest("수정된 수령인", "01012345678", 12345, "서울시", "건물명", "101호", 4000, "UPDATED_CODE");
        Order order = Order.builder().id(1L).build();
        Shipment shipment = Shipment.builder().id(1L).order(order).build();
        ShipmentResponse response = ShipmentResponse.from(shipment);
        when(shipmentService.updateShipment(anyLong(), any())).thenReturn(response);

        // when, then
        mockMvc.perform(put("/shipments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("배송 수정 - 실패")
    void updateShipment_실패() throws Exception {
        // given
        ShipmentUpdateRequest request = new ShipmentUpdateRequest("수정된 수령인", "01012345678", 12345, "서울시", "건물명", "101호", 4000, "UPDATED_CODE");
        when(shipmentService.updateShipment(anyLong(), any())).thenThrow(new ShipmentNotFoundException());


        // when, then
        mockMvc.perform(put("/shipments/1")
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
    @DisplayName("배송 완료 시간 변경 - 성공")
    void updateShipmentDeliveredAt_성공() throws Exception {
        // given
        Order order = Order.builder().id(1L).build();
        Shipment shipment = Shipment.builder().id(1L).order(order).build();
        ShipmentResponse response = ShipmentResponse.from(shipment);
        when(shipmentService.updateShipmentDeliveredAt(anyLong(), any())).thenReturn(response);

        // when, then
        mockMvc.perform(patch("/shipments/1/delivered-at")
                        .param("deliveredAt", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("배송 완료 시간 변경 - 실패")
    void updateShipmentDeliveredAt_실패() throws Exception {
        // given
        when(shipmentService.updateShipmentDeliveredAt(anyLong(), any())).thenThrow(new ShipmentNotFoundException());

        // when, then
        mockMvc.perform(patch("/shipments/1/delivered-at")
                        .param("deliveredAt", LocalDateTime.now().toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }


    @Test
    @DisplayName("배송 삭제 - 성공")
    void deleteShipment_성공() throws Exception {
        // given
        doNothing().when(shipmentService).deleteShipment(anyLong());

        // when, then
        mockMvc.perform(delete("/shipments/1"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("배송 삭제 - 실패")
    void deleteShipment_실패() throws Exception {
        // given
        doThrow(new ShipmentNotFoundException(1L)).when(shipmentService).deleteShipment(anyLong());

        // when, then
        mockMvc.perform(delete("/shipments/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}