package shop.ink3.api.review.review.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.review.review.dto.ReviewListResponse;
import shop.ink3.api.review.review.dto.ReviewRequest;
import shop.ink3.api.review.review.dto.ReviewResponse;
import shop.ink3.api.review.review.dto.ReviewUpdateRequest;
import shop.ink3.api.review.review.service.ReviewService;

@WebMvcTest(ReviewController.class)
@ExtendWith(SpringExtension.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("리뷰 등록")
    void addReview() throws Exception {
        ReviewRequest request = new ReviewRequest(1L, 1L, "유익해요", "많은 도움이 되었어요!", 5);
        ReviewResponse response = new ReviewResponse(1L, 1L, 1L, 1L, "유익해요", "많은 도움이 되었어요!", 5, LocalDateTime.now(), LocalDateTime.now(), null, null);
        MockMultipartFile reviewPart = new MockMultipartFile("review", "", "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile imagePart = new MockMultipartFile("images", "image.png", "image/png", "test image".getBytes());

        when(reviewService.addReview(any(), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/reviews")
                .file(reviewPart)
                .file(imagePart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.content").value("많은 도움이 되었어요!"));
    }

    @Test
    @DisplayName("리뷰 삭제")
    void deleteReview() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/reviews/1"))
            .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L);
    }

    @Test
    @DisplayName("사용자 리뷰 목록 조회")
    void getReviewByUserId() throws Exception {
        PageResponse<ReviewListResponse> pageResponse = PageResponse.from(
            new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        );
        when(reviewService.getReviewsByUserId(any(), eq(1L))).thenReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/1/reviews"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("도서 리뷰 목록 조회")
    void getReviewsByBookId() throws Exception {
        PageResponse<ReviewListResponse> pageResponse = PageResponse.from(
            new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        );
        when(reviewService.getReviewsByBookId(any(), eq(1L))).thenReturn(pageResponse);

        mockMvc.perform(MockMvcRequestBuilders.get("/books/1/reviews"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리뷰 수정")
    void updateReview() throws Exception {
        ReviewUpdateRequest updateRequest = new ReviewUpdateRequest("수정된 제목", "수정된 내용", 4);
        ReviewResponse response = new ReviewResponse(1L, 1L, 1L, 1L, "유익해요", "많은 도움이 되었어요!", 4, LocalDateTime.now(), LocalDateTime.now(), null, null);
        MockMultipartFile reviewPart = new MockMultipartFile("review", "", "application/json", objectMapper.writeValueAsBytes(updateRequest));

        when(reviewService.updateReview(eq(1L), any(), any(), eq(123L))).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/reviews/1")
                .file(reviewPart)
                .param("userId", "123")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rating").value(4));
    }
}
