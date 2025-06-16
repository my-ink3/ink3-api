package shop.ink3.api.review.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.common.uploader.MinioService;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.orderBook.entity.OrderBook;
import shop.ink3.api.order.orderBook.repository.OrderBookRepository;
import shop.ink3.api.review.review.dto.ReviewDefaultListResponse;
import shop.ink3.api.review.review.dto.ReviewListResponse;
import shop.ink3.api.review.review.dto.ReviewRequest;
import shop.ink3.api.review.review.dto.ReviewResponse;
import shop.ink3.api.review.review.dto.ReviewUpdateRequest;
import shop.ink3.api.review.review.entity.Review;
import shop.ink3.api.review.review.exception.ReviewNotFoundException;
import shop.ink3.api.review.review.repository.ReviewRepository;
import shop.ink3.api.review.reviewImage.entity.ReviewImage;
import shop.ink3.api.review.reviewImage.repository.ReviewImageRepository;
import shop.ink3.api.user.point.history.entity.PointHistory;
import shop.ink3.api.user.point.history.entity.PointHistoryStatus;
import shop.ink3.api.user.point.history.service.PointService;
import shop.ink3.api.user.point.policy.dto.PointPolicyResponse;
import shop.ink3.api.user.point.policy.service.PointPolicyService;
import shop.ink3.api.user.user.entity.User;
import shop.ink3.api.user.user.repository.UserRepository;

class ReviewServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OrderBookRepository orderBookRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private PointService pointService;

    @Mock
    private PointPolicyService pointPolicyService;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Order order;
    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder().id(1L).name("user1").build();
        order = Order.builder().id(1L).user(user).build();
        Book book = Book.builder()
            .id(1L)
            .totalRating(0L)
            .reviewCount(0L)
            .likeCount(0L)
            .build();

        orderBook = OrderBook.builder()
            .id(1L)
            .book(book)
            .order(order)
            .build();

        when(pointPolicyService.getPointPolicy(anyLong())).thenReturn(
            new PointPolicyResponse(
                1L,
                "기본정책",
                100,
                200,
                300,
                5,
                true,
                LocalDateTime.now()
            )
        );

        when(pointService.earnPoint(anyLong(), any())).thenReturn(
            PointHistory.builder()
                .id(1L)
                .user(user)
                .delta(100)
                .status(PointHistoryStatus.EARN)
                .description("100포인트가 적립되었습니다.")
                .createdAt(LocalDateTime.now())
                .build()
        );

        ReflectionTestUtils.setField(reviewService, "bucket", "test-bucket");
    }

    @Test
    @DisplayName("리뷰 등록")
    void addReview() {
        ReviewRequest request = new ReviewRequest(1L, 1L, "제목", "내용", 5);
        Review review = new Review(user, orderBook, "제목", "내용", 5);
        ReflectionTestUtils.setField(review, "id", 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderBookRepository.findById(1L)).thenReturn(Optional.of(orderBook));
        when(reviewRepository.existsByOrderBookId(1L)).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(review);
        when(reviewImageRepository.findByReviewId(1L)).thenReturn(List.of());

        ReviewResponse response = reviewService.addReview(request, List.of());

        assertThat(orderBook.getOrder()).isNotNull();
        assertThat(orderBook.getOrder().getUser()).isEqualTo(user);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.images()).isEmpty();
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 사용자 없음")
    void addReviewFail_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ReviewRequest request = new ReviewRequest(1L, 1L, "제목", "내용", 5);

        assertThatThrownBy(() -> reviewService.addReview(request, List.of()))
            .isInstanceOf(shop.ink3.api.user.user.exception.UserNotFoundException.class);
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 주문 도서 없음")
    void addReviewFail_orderBookNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderBookRepository.findById(1L)).thenReturn(Optional.empty());

        ReviewRequest request = new ReviewRequest(1L, 1L, "제목", "내용", 5);

        assertThrows(shop.ink3.api.order.orderBook.exception.OrderBookNotFoundException.class,
            () -> reviewService.addReview(request, List.of()));
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 본인의 주문이 아님")
    void addReviewFail_unauthorizedUser() {
        User anotherUser = User.builder().id(999L).build();
        Order anotherOrder = Order.builder().id(2L).user(anotherUser).build();
        OrderBook anotherOrderBook = OrderBook.builder().id(1L).order(anotherOrder).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderBookRepository.findById(1L)).thenReturn(Optional.of(anotherOrderBook));

        ReviewRequest request = new ReviewRequest(1L, 1L, "제목", "내용", 5);

        assertThrows(shop.ink3.api.review.review.exception.UnauthorizedOrderBookAccessException.class,
            () -> reviewService.addReview(request, List.of()));
    }

    @Test
    @DisplayName("리뷰 등록 실패 - 이미 리뷰 존재")
    void addReviewFail_duplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderBookRepository.findById(1L)).thenReturn(Optional.of(orderBook));
        when(reviewRepository.existsByOrderBookId(1L)).thenReturn(true);

        ReviewRequest request = new ReviewRequest(1L, 1L, "제목", "내용", 5);

        assertThrows(shop.ink3.api.review.review.exception.ReviewAlreadyRegisterException.class,
            () -> reviewService.addReview(request, List.of()));
    }

    @Test
    @DisplayName("리뷰 수정")
    void updateReview() {
        Review review = new Review(user, orderBook, "제목", "내용", 3);
        ReflectionTestUtils.setField(review, "id", 1L);

        ReviewUpdateRequest request = new ReviewUpdateRequest("제목 수정", "내용 수정", 4);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewId(1L)).thenReturn(List.of());
        when(minioService.upload(any(MultipartFile.class), anyString())).thenReturn("new-image.jpg");

        ReviewResponse response = reviewService.updateReview(1L, request, List.of(mock(MultipartFile.class)), 1L);

        assertThat(response.title()).isEqualTo("제목 수정");
        assertThat(response.rating()).isEqualTo(4);
    }

    @Test
    @DisplayName("도서 ID로 리뷰 목록 조회")
    void getReviewsByBookId() {
        ReviewDefaultListResponse dto = new ReviewDefaultListResponse(1L, 1L, 1L, 1L, "user1", "제목", "내용", 5, null,
            null);
        Page<ReviewDefaultListResponse> page = new PageImpl<>(List.of(dto));

        Review review = new Review(user, orderBook, "제목", "내용", 5);
        ReflectionTestUtils.setField(review, "id", 1L);

        ReviewImage image = ReviewImage.builder()
            .imageUrl("img1.jpg")
            .review(review)
            .build();

        when(reviewRepository.findListByBookId(any(), anyLong())).thenReturn(page);
        when(reviewImageRepository.findByReviewIdIn(List.of(1L))).thenReturn(List.of(image));
        when(minioService.getPresignedUrl(anyString(), anyString()))
            .thenReturn("http://storage.java21.net:8000/ink3-dev-reviews-images/sample.jpg");

        PageResponse<ReviewListResponse> response = reviewService.getReviewsByBookId(PageRequest.of(0, 10), 1L);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().images()).hasSize(1);
    }

    @Test
    @DisplayName("유저의 리뷰 목록 조회")
    void getReviewsByUserId() {
        ReviewDefaultListResponse dto = new ReviewDefaultListResponse(
            1L, 1L, 1L, 1L, "user1", "제목", "내용", 5, null, null
        );
        Page<ReviewDefaultListResponse> page = new PageImpl<>(List.of(dto));

        Review review = new Review(user, orderBook, "제목", "내용", 5);
        ReflectionTestUtils.setField(review, "id", 1L);

        ReviewImage image = ReviewImage.builder()
            .imageUrl("img1.jpg")
            .review(review)
            .build();

        when(reviewRepository.findListByUserId(any(), anyLong())).thenReturn(page);
        when(reviewImageRepository.findByReviewIdIn(List.of(1L))).thenReturn(List.of(image));
        when(minioService.getPresignedUrl(anyString(), anyString()))
            .thenReturn("http://storage.java21.net:8000/ink3-dev-reviews-images/sample.jpg");

        PageResponse<ReviewListResponse> response = reviewService.getReviewsByUserId(PageRequest.of(0, 10), 1L);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).images()).hasSize(1);
    }

    @Test
    @DisplayName("리뷰 수정 - 예외")
    void updateReviewFail() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class,
            () -> reviewService.updateReview(1L, new ReviewUpdateRequest("제목", "내용", 3), null, 1L));
    }

    @Test
    @DisplayName("리뷰 삭제")
    void deleteReview() {
        Review review = new Review(user, orderBook, "제목", "내용", 5);
        ReflectionTestUtils.setField(review, "id", 1L);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewImageRepository.findByReviewId(1L)).thenReturn(List.of());

        reviewService.deleteReview(1L);

        verify(reviewRepository).deleteById(1L);
    }

    @Test
    @DisplayName("리뷰 삭제 - 예외")
    void deleteReviewFail() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ReviewNotFoundException.class, () -> reviewService.deleteReview(1L));
    }
}
