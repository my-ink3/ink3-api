package shop.ink3.api.review.review.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.review.review.dto.ReviewListResponse;
import shop.ink3.api.review.review.dto.ReviewRequest;
import shop.ink3.api.review.review.dto.ReviewResponse;
import shop.ink3.api.review.review.dto.ReviewUpdateRequest;
import shop.ink3.api.review.review.service.ReviewService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/me")
public class MeReviewController {
    private final ReviewService reviewService;

    @PostMapping(value = "/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ReviewResponse>> addReview(@RequestHeader(name = "X-User-Id") Long userId,
        // @RequestPart("review") @Valid MeReviewRequest request,
        @RequestParam Long orderBookId,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam int rating,
        @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ReviewRequest reviewRequest = new ReviewRequest(userId, orderBookId, title, content, rating);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CommonResponse.create(reviewService.addReview(reviewRequest, images)));
    }

    @PostMapping(value = "/reviews/{review-id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ReviewResponse>> updateReview(@RequestHeader(name = "X-User-Id") Long userId,
        @PathVariable(name = "review-id") Long reviewId,
        // @RequestPart("review") @Valid ReviewUpdateRequest reviewUpdateRequest,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam int rating,
        @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(
            CommonResponse.update(
                reviewService.updateReview(reviewId, new ReviewUpdateRequest(title, content, rating), images, userId)));
    }

    @GetMapping("/reviews")
    public ResponseEntity<PageResponse<ReviewListResponse>> getReviewsByUserId(
        @RequestHeader(name = "X-User-Id") Long userId,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByUserId(pageable, userId));
    }

    @GetMapping("/books/{book-id}/reviews")
    public ResponseEntity<PageResponse<ReviewListResponse>> getReviewsByBookId(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @PathVariable(name = "book-id") Long bookId) {
        return ResponseEntity.ok(reviewService.getReviewsByBookId(pageable, bookId));
    }

    @DeleteMapping("/reviews/{review-id}")
    public ResponseEntity<Void> deleteReview(@RequestHeader(name = "X-User-Id") Long userId,
        @PathVariable(name = "review-id") Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
