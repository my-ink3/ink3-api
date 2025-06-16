package shop.ink3.api.review.review.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.review.review.dto.ReviewListResponse;
import shop.ink3.api.review.review.dto.ReviewResponse;
import shop.ink3.api.review.review.service.ReviewService;
import shop.ink3.api.review.reviewImage.dto.ReviewImageResponse;

@WebMvcTest(MeReviewController.class)
class MeReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    private ReviewResponse reviewResponse;
    private ReviewListResponse reviewListResponse;

    @BeforeEach
    void setUp() {
        reviewResponse = new ReviewResponse(1L, 1L, 1L, 1L, "제목", "내용", 5,
            LocalDateTime.now(), LocalDateTime.now(),
            List.of("image1.jpg", "image2.jpg"), null);

        reviewListResponse = new ReviewListResponse(1L, 1L, 1L, 1L, "user1", "제목", "내용", 5,
            LocalDateTime.now(), LocalDateTime.now(),
            List.of(new ReviewImageResponse("image1.jpg"), new ReviewImageResponse("image2.jpg")));
    }

    @Test
    @DisplayName("리뷰 등록")
    void addReview() throws Exception {
        MockMultipartFile file = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "test".getBytes());

        when(reviewService.addReview(any(), any())).thenReturn(reviewResponse);

        mockMvc.perform(multipart("/me/reviews")
                .file(file)
                .param("orderBookId", "1")
                .param("title", "제목")
                .param("content", "내용")
                .param("rating", "5")
                .header("X-User-Id", "1")
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("제목"))
            .andExpect(jsonPath("$.data.images.length()").value(2))
            .andExpect(jsonPath("$.data.images[0]").value("image1.jpg"));
    }

    @Test
    @DisplayName("이미지 없이 리뷰 등록")
    void addReviewWithoutImage() throws Exception {
        when(reviewService.addReview(any(), any())).thenReturn(reviewResponse);

        mockMvc.perform(multipart("/me/reviews")
                .param("orderBookId", "1")
                .param("title", "제목")
                .param("content", "내용")
                .param("rating", "5")
                .header("X-User-Id", "1")
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    @DisplayName("리뷰 수정")
    void updateReview() throws Exception {
        ReviewResponse updatedResponse = new ReviewResponse(1L, 1L, 1L, 1L, "제목 수정", "제목 내용 수정", 4,
            LocalDateTime.now(), LocalDateTime.now(),
            List.of("image3.jpg", "image4.jpg"), null);

        MockMultipartFile file = new MockMultipartFile("images", "image3.jpg", "image/jpeg", "test".getBytes());

        when(reviewService.updateReview(eq(1L), any(), any(), eq(1L))).thenReturn(updatedResponse);

        mockMvc.perform(multipart("/me/reviews/1")
                .file(file)
                .param("title", "제목 수정")
                .param("content", "제목 내용 수정")
                .param("rating", "4")
                .header("X-User-Id", "1")
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("제목 수정"))
            .andExpect(jsonPath("$.data.images[0]").value("image3.jpg"));
    }

    @Test
    @DisplayName("도서 ID로 리뷰 목록 조회")
    void getReviewsByBookId() throws Exception {
        PageResponse<ReviewListResponse> pageResponse = PageResponse.from(
            new PageImpl<>(List.of(reviewListResponse), PageRequest.of(0, 10), 1)
        );

        when(reviewService.getReviewsByBookId(any(), eq(1L))).thenReturn(pageResponse);

        mockMvc.perform(get("/me/books/1/reviews?page=0&size=10&sort=createdAt,DESC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("제목"))
            .andExpect(jsonPath("$.content[0].images[0].imageUrl").value("image1.jpg"));
    }

    @Test
    @DisplayName("유저의 리뷰 목록 조회")
    void getReviewsByUserId() throws Exception {
        PageResponse<ReviewListResponse> pageResponse = PageResponse.from(
            new PageImpl<>(List.of(reviewListResponse), PageRequest.of(0, 10), 1)
        );

        when(reviewService.getReviewsByUserId(any(), eq(1L))).thenReturn(pageResponse);

        mockMvc.perform(get("/me/reviews?page=0&size=10&sort=createdAt,DESC")
                .header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("제목"))
            .andExpect(jsonPath("$.content[0].images[0].imageUrl").value("image1.jpg"));
    }

    @Test
    @DisplayName("리뷰 삭제")
    void deleteReview() throws Exception {
        mockMvc.perform(delete("/me/reviews/1")
                .header("X-User-Id", "1"))
            .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L);
    }
}
