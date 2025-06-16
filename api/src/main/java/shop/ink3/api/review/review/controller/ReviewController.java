package shop.ink3.api.review.review.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.review.review.dto.ReviewListResponse;
import shop.ink3.api.review.review.dto.ReviewRequest;
import shop.ink3.api.review.review.dto.ReviewResponse;
import shop.ink3.api.review.review.dto.ReviewUpdateRequest;
import shop.ink3.api.review.review.service.ReviewService;

@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping(value = "/reviews", consumes = {"multipart/form-data"})
    public ResponseEntity<CommonResponse<ReviewResponse>> addReview(
        @RequestPart("review") @Valid ReviewRequest reviewRequest,
        @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CommonResponse.create(reviewService.addReview(reviewRequest, images)));
    }

    @PutMapping(value = "/reviews/{review-id}", consumes = {"multipart/form-data"})
    public ResponseEntity<CommonResponse<ReviewResponse>> updateReview(@PathVariable(name = "review-id") Long reviewId,
        @RequestPart("review") @Valid ReviewUpdateRequest reviewUpdateRequest,
        @RequestPart(value = "images", required = false) List<MultipartFile> images, @RequestParam Long userId) {
        return ResponseEntity.ok(
            CommonResponse.update(reviewService.updateReview(reviewId, reviewUpdateRequest, images, userId)));
    }

    @GetMapping("/users/{user-id}/reviews")
    public ResponseEntity<CommonResponse<PageResponse<ReviewListResponse>>> getReviewByUserId(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @PathVariable(name = "user-id") Long userId) {
        return ResponseEntity.ok(CommonResponse.success(reviewService.getReviewsByUserId(pageable, userId)));
    }

    @GetMapping("/books/{book-id}/reviews")
    public ResponseEntity<PageResponse<ReviewListResponse>> getReviewsByBookId(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @PathVariable(name = "book-id") Long bookId) {
        return ResponseEntity.ok(reviewService.getReviewsByBookId(pageable, bookId));
    }

    @DeleteMapping("/reviews/{review-id}")
    public ResponseEntity<Void> deleteReview(@PathVariable(name = "review-id") Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
