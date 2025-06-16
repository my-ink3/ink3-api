package shop.ink3.api.user.like.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import shop.ink3.api.user.like.dto.LikeCreateRequest;
import shop.ink3.api.user.like.dto.LikeResponse;
import shop.ink3.api.user.like.service.LikeService;

@WebMvcTest(MeLikeController.class)
@ExtendWith(SpringExtension.class)
class MeLikeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LikeService likeService;

    @Test
    @DisplayName("내 좋아요 생성")
    void createCurrentUserLike() throws Exception {
        LikeCreateRequest request = new LikeCreateRequest(1L);
        LikeResponse response = new LikeResponse(1L, 1L, 1L, null, null, 0, 0, 0, null);

        when(likeService.createLike(any(Long.class), any(LikeCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users/me/likes")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.bookId").value(1L));
    }

    @Test
    @DisplayName("내 좋아요 목록 조회")
    void getCurrentUserLikes() throws Exception {
        LikeResponse response = new LikeResponse(1L, 1L, 1L, null, null, 0, 0, 0, null);
        PageResponse<LikeResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of(response)));

        when(likeService.getLikes(any(Long.class), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/users/me/likes").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].bookId").value(1L));
    }

    @Test
    @DisplayName("내 좋아요 삭제")
    void deleteCurrentUserLike() throws Exception {
        doNothing().when(likeService).deleteLike(1L, 1L);

        mockMvc.perform(delete("/users/me/likes/1").header("X-User-Id", 1L))
            .andExpect(status().isNoContent());
    }
}
