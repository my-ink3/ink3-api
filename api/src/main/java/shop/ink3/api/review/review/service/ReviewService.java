package shop.ink3.api.review.review.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.common.uploader.MinioService;
import shop.ink3.api.order.orderBook.entity.OrderBook;
import shop.ink3.api.order.orderBook.exception.OrderBookNotFoundException;
import shop.ink3.api.order.orderBook.repository.OrderBookRepository;
import shop.ink3.api.review.review.dto.ReviewDefaultListResponse;
import shop.ink3.api.review.review.dto.ReviewListResponse;
import shop.ink3.api.review.review.dto.ReviewRequest;
import shop.ink3.api.review.review.dto.ReviewResponse;
import shop.ink3.api.review.review.dto.ReviewUpdateRequest;
import shop.ink3.api.review.review.entity.Review;
import shop.ink3.api.review.review.exception.ReviewAlreadyRegisterException;
import shop.ink3.api.review.review.exception.ReviewNotFoundException;
import shop.ink3.api.review.review.exception.UnauthorizedOrderBookAccessException;
import shop.ink3.api.review.review.repository.ReviewRepository;
import shop.ink3.api.review.reviewImage.dto.ReviewImageMapping;
import shop.ink3.api.review.reviewImage.dto.ReviewImageResponse;
import shop.ink3.api.review.reviewImage.entity.ReviewImage;
import shop.ink3.api.review.reviewImage.repository.ReviewImageRepository;
import shop.ink3.api.user.point.history.entity.PointHistory;
import shop.ink3.api.user.point.history.service.PointService;
import shop.ink3.api.user.point.policy.dto.PointPolicyResponse;
import shop.ink3.api.user.point.policy.service.PointPolicyService;
import shop.ink3.api.user.user.dto.UserPointRequest;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.exception.UserNotFoundException;
import shop.ink3.api.user.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private static final String POINT_REVIEW = "리뷰 작성에 대한 적립";

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderBookRepository orderBookRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final PointPolicyService pointPolicyService;
    private final PointService pointService;
    private final MinioService minioService;

    @Value("${minio.review-bucket}")
    private String bucket;

    public ReviewResponse addReview(ReviewRequest request, List<MultipartFile> images) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));
        OrderBook orderBook = orderBookRepository.findById(request.orderBookId())
                .orElseThrow(() -> new OrderBookNotFoundException(request.orderBookId()));

        if (!orderBook.getOrder().getUser().getId().equals(request.userId())) {
            throw new UnauthorizedOrderBookAccessException(user.getId(), orderBook.getId());
        }

        if (reviewRepository.existsByOrderBookId(orderBook.getId())) {
            throw new ReviewAlreadyRegisterException(orderBook.getId());
        }

        Book book = orderBook.getBook();
        book.addRating(request.rating());
        Long reviewCount = reviewRepository.countByOrderBookBookId(book.getId());
        book.updateReviewCount(reviewCount);
        bookRepository.save(book);

        Review review = Review.builder()
                .user(user)
                .orderBook(orderBook)
                .title(request.title())
                .content(request.content())
                .rating(request.rating())
                .build();
        Review savedReview = reviewRepository.save(review);

        PointPolicyResponse response = pointPolicyService.getPointPolicy(1);
        PointHistory pointHistory = getPointHistory(images, user, response);

        List<String> imageUrls = saveImages(images, savedReview);

        return ReviewResponse.from(savedReview, imageUrls, pointHistory.getDescription());
    }

    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images,
                                       Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedOrderBookAccessException(userId, review.getOrderBook().getId());
        }

        int oldRating = review.getRating();
        int newRating = request.getRating();
        review.update(request.getTitle(), request.getContent(), newRating);

        Book book = review.getOrderBook().getBook();
        book.updateRating(oldRating, newRating);
        bookRepository.save(book);

        List<String> imageUrls;
        if (images != null && images.stream().anyMatch(image -> !image.isEmpty())) {
            List<ReviewImage> existingImages = reviewImageRepository.findByReviewId(review.getId());
            for (ReviewImage image : existingImages) {
                minioService.delete(image.getImageUrl(), bucket);
                reviewImageRepository.deleteById(image.getId());
            }

            imageUrls = saveImages(images, review);
        } else {
            imageUrls = reviewImageRepository.findByReviewId(review.getId()).stream()
                    .map(ReviewImage::getImageUrl)
                    .map(url -> minioService.getPresignedUrl(url, bucket))
                    .toList();
        }

        return ReviewResponse.from(review, imageUrls);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListResponse> getReviewsByUserId(Pageable pageable, Long userId) {
        Page<ReviewDefaultListResponse> page = reviewRepository.findListByUserId(pageable, userId);
        return getReviewListResponsePage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewListResponse> getReviewsByBookId(Pageable pageable, Long bookId) {
        Page<ReviewDefaultListResponse> page = reviewRepository.findListByBookId(pageable, bookId);
        return getReviewListResponsePage(page);
    }

    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        List<ReviewImage> images = reviewImageRepository.findByReviewId(review.getId());

        for (ReviewImage image : images) {
            minioService.delete(image.getImageUrl(), bucket);
            reviewImageRepository.deleteById(image.getId());
        }

        reviewRepository.deleteById(reviewId);
    }

    private PointHistory getPointHistory(List<MultipartFile> images, User user, PointPolicyResponse response) {
        Integer point;
        if (images != null && !images.isEmpty()) {
            point = response.imageReviewPoint();
        } else {
            point = response.reviewPoint();
        }
        return pointService.earnPoint(user.getId(),
                new UserPointRequest(point, POINT_REVIEW));
    }

    private List<String> saveImages(List<MultipartFile> images, Review review) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .map(image -> {
                    String imageUrl = minioService.upload(image, bucket);
                    reviewImageRepository.save(
                            ReviewImage.builder()
                                    .review(review)
                                    .imageUrl(imageUrl)
                                    .build()
                    );
                    return imageUrl;
                })
                .toList();
    }

    private PageResponse<ReviewListResponse> getReviewListResponsePage(Page<ReviewDefaultListResponse> page) {
        List<Long> reviewIds = page.getContent().stream()
                .map(ReviewDefaultListResponse::id)
                .toList();

        Map<Long, List<String>> imageMap = reviewImageRepository.findByReviewIdIn(reviewIds).stream()
                .map(image -> new ReviewImageMapping(image.getReview().getId(), image.getImageUrl()))
                .collect(Collectors.groupingBy(
                        ReviewImageMapping::reviewId,
                        Collectors.mapping(ReviewImageMapping::imageUrl, Collectors.toList())
                ));

        Page<ReviewListResponse> mappedPage = page.map(dto -> {
            List<ReviewImageResponse> images = imageMap.getOrDefault(dto.id(), List.of()).stream()
                    .map(url -> {
                        return new ReviewImageResponse(minioService.getPresignedUrl(url, bucket));
                    })
                    .toList();

            return new ReviewListResponse(
                    dto.id(),
                    dto.userId(),
                    dto.bookId(),
                    dto.orderBookId(),
                    dto.userName(),
                    dto.title(),
                    dto.content(),
                    dto.rating(),
                    dto.createdAt(),
                    dto.modifiedAt(),
                    images
            );
        });

        return PageResponse.from(mappedPage);
    }
}
