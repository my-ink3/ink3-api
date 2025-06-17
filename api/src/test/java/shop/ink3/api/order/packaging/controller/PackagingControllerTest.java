package shop.ink3.api.order.packaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import shop.ink3.api.order.packaging.dto.PackagingCreateRequest;
import shop.ink3.api.order.packaging.dto.PackagingResponse;
import shop.ink3.api.order.packaging.dto.PackagingUpdateRequest;
import shop.ink3.api.order.packaging.entity.Packaging;
import shop.ink3.api.order.packaging.exception.PackagingNotFoundException;
import shop.ink3.api.order.packaging.service.PackagingService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PackagingController.class)
class PackagingControllerTest {

    @MockitoBean
    PackagingService packagingService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("포장 정책 단건 조회 - 성공")
    void getPackaging_성공() throws Exception {
        Packaging packaging = Packaging.builder().id(1L).name("테스트").build();
        PackagingResponse response = PackagingResponse.from(packaging);
        when(packagingService.getPackaging(1L)).thenReturn(response);

        mockMvc.perform(get("/packagings/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("포장 정책 단건 조회 - 실패")
    void getPackaging_실패() throws Exception {
        when(packagingService.getPackaging(1L)).thenThrow(new PackagingNotFoundException(1L));

        mockMvc.perform(get("/packagings/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 목록 조회 - 성공")
    void getPackagingList_성공() throws Exception {
        PageResponse<PackagingResponse> pageResponse = new PageResponse<>(
                List.of(
                        PackagingResponse.from(Packaging.builder().id(1L).name("테스트1").build()),
                        PackagingResponse.from(Packaging.builder().id(2L).name("테스트2").build())
                ),
                0, 2, 2L, 1, false, false
        );
        when(packagingService.getPackagingList(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/packagings"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 목록 조회 - 실패")
    void getPackagingList_실패() throws Exception {
        when(packagingService.getPackagingList(any())).thenThrow(new PackagingNotFoundException());

        mockMvc.perform(get("/packagings"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("활성화된 포장 정책 목록 조회 - 성공")
    void getAvailablePackagingList_성공() throws Exception {
        PageResponse<PackagingResponse> pageResponse = new PageResponse<>(
                List.of(
                        PackagingResponse.from(Packaging.builder().id(1L).name("테스트1").build()),
                        PackagingResponse.from(Packaging.builder().id(2L).name("테스트2").build())
                ),
                0, 2, 2L, 1, false, false
        );
        when(packagingService.getAvailablePackagingList(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/packagings/activate")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andDo(print());
    }

    @Test
    @DisplayName("활성화된 포장 정책 목록 조회 - 실패")
    void getAvailablePackagingList_실패() throws Exception {
        when(packagingService.getAvailablePackagingList(any())).thenThrow(new PackagingNotFoundException());

        mockMvc.perform(get("/packagings/activate"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 생성 - 성공")
    void createPackaging_성공() throws Exception {
        PackagingCreateRequest request = new PackagingCreateRequest("테스트", 1000);
        Packaging packaging = Packaging.builder().id(1L).name("테스트").price(1000).build();
        PackagingResponse response = PackagingResponse.from(packaging);
        when(packagingService.createPackaging(any())).thenReturn(response);

        mockMvc.perform(post("/packagings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 생성 - 실패")
    void createPackaging_실패() throws Exception {
        PackagingCreateRequest request = new PackagingCreateRequest("테스트", 1000);
        when(packagingService.createPackaging(any())).thenThrow(new PackagingNotFoundException(1L));

        mockMvc.perform(post("/packagings")
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
    @DisplayName("포장 정책 수정 - 성공")
    void updatePackaging_성공() throws Exception {
        PackagingUpdateRequest request = new PackagingUpdateRequest("변경전", 2000);
        Packaging packaging = Packaging.builder().id(1L).name("변경후").price(2000).build();
        PackagingResponse response = PackagingResponse.from(packaging);
        when(packagingService.updatePackaging(anyLong(), any())).thenReturn(response);

        mockMvc.perform(put("/packagings/1")
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
    @DisplayName("포장 정책 수정 - 실패")
    void updatePackaging_실패() throws Exception {
        PackagingUpdateRequest request = new PackagingUpdateRequest("수정된 정책", 2000);
        when(packagingService.updatePackaging(anyLong(), any())).thenThrow(new PackagingNotFoundException(1L));

        mockMvc.perform(put("/packagings/1")
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
    @DisplayName("포장 정책 삭제 - 성공")
    void deletePackaging_성공() throws Exception {
        doNothing().when(packagingService).deletePackaging(anyLong());

        mockMvc.perform(delete("/packagings/1"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 삭제 - 실패")
    void deletePackaging_실패() throws Exception {
        doThrow(new PackagingNotFoundException(1L)).when(packagingService).deletePackaging(anyLong());

        mockMvc.perform(delete("/packagings/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 활성화 - 성공")
    void activatePackaging_성공() throws Exception {
        doNothing().when(packagingService).activate(anyLong());

        mockMvc.perform(patch("/packagings/1/activate"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 활성화 - 실패")
    void activatePackaging_실패() throws Exception {
        doThrow(new PackagingNotFoundException(1L)).when(packagingService).activate(anyLong());

        mockMvc.perform(patch("/packagings/1/activate"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 비활성화 - 성공")
    void deactivatePackaging_성공() throws Exception {
        doNothing().when(packagingService).deactivate(anyLong());

        mockMvc.perform(patch("/packagings/1/deactivate"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("포장 정책 비활성화 - 실패")
    void deactivatePackaging_실패() throws Exception {
        doThrow(new PackagingNotFoundException(1L)).when(packagingService).deactivate(anyLong());

        mockMvc.perform(patch("/packagings/1/deactivate"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andDo(print());
    }
}