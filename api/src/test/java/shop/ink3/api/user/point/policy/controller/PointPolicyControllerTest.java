package shop.ink3.api.user.point.policy.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.user.point.policy.dto.PointPolicyCreateRequest;
import shop.ink3.api.user.point.policy.dto.PointPolicyResponse;
import shop.ink3.api.user.point.policy.dto.PointPolicyUpdateRequest;
import shop.ink3.api.user.point.policy.service.PointPolicyService;

@WebMvcTest(PointPolicyController.class)
@ExtendWith(SpringExtension.class)
public class PointPolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointPolicyService pointPolicyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("포인트 정책 생성")
    void createPointPolicy() throws Exception {
        PointPolicyCreateRequest request = new PointPolicyCreateRequest("가입 포인트", 1000, 500, 800, 10);
        PointPolicyResponse response = new PointPolicyResponse(1L, "가입 포인트", 1000, 500, 800, 10, false, LocalDateTime.now());

        when(pointPolicyService.createPointPolicy(any())).thenReturn(response);

        mockMvc.perform(post("/point-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("가입 포인트"));
    }

    @Test
    @DisplayName("포인트 정책 목록 조회")
    void getPointPolicies() throws Exception {
        PointPolicyResponse response = new PointPolicyResponse(1L, "정책", 1000, 500, 800, 10, false, LocalDateTime.now());
        PageResponse<PointPolicyResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of(response)));

        when(pointPolicyService.getPointPolicies(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/point-policies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].name").value("정책"));
    }

    @Test
    @DisplayName("포인트 정책 활성화")
    void activatePointPolicy() throws Exception {
        when(pointPolicyService.activatePointPolicy(1L)).thenReturn(new PointPolicyResponse(1L, "활성화 정책", 1000, 500, 800, 10, true, LocalDateTime.now()));

        mockMvc.perform(patch("/point-policies/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("포인트 정책 삭제")
    void deletePointPolicy() throws Exception {
        doNothing().when(pointPolicyService).deletePointPolicy(1L);

        mockMvc.perform(delete("/point-policies/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("포인트 정책 수정")
    void updatePointPolicy() throws Exception {
        PointPolicyUpdateRequest request = new PointPolicyUpdateRequest("변경된 정책", 1000, 500, 800, 10);
        PointPolicyResponse response = new PointPolicyResponse(1L, "변경된 정책", 1000, 500, 800, 10, false, LocalDateTime.now());

        when(pointPolicyService.updatePointPolicy(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/point-policies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("변경된 정책"));
    }
}
