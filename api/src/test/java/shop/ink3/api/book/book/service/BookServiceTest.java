package shop.ink3.api.book.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import shop.ink3.api.book.book.dto.BookAuthorDto;
import shop.ink3.api.book.book.dto.BookCreateRequest;
import shop.ink3.api.book.book.dto.BookDetailResponse;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.entity.BookStatus;
import shop.ink3.api.book.book.enums.SortType;
import shop.ink3.api.book.book.exception.BookNotFoundException;
import shop.ink3.api.book.book.exception.DuplicateIsbnException;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.book.bookauthor.repository.BookAuthorRepository;
import shop.ink3.api.book.bookcategory.repository.BookCategoryRepository;
import shop.ink3.api.book.booktag.repository.BookTagRepository;
import shop.ink3.api.book.publisher.entity.Publisher;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.review.review.repository.ReviewRepository;
import shop.ink3.api.user.like.repository.LikeRepository;

class BookServiceTest {
    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private BookCategoryRepository bookCategoryRepository;

    @Mock
    private BookAuthorRepository bookAuthorRepository;

    @Mock
    private BookTagRepository bookTagRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private Publisher publisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publisher = Publisher.builder().name("출판사").build();
        book = Book.builder()
                .id(1L)
                .isbn("1234567890123")
                .title("책 제목")
                .contents("목차")
                .description("설명")
                .publishedAt(LocalDate.of(2024, 1, 1))
                .originalPrice(20000)
                .salePrice(18000)
                .status(BookStatus.AVAILABLE)
                .isPackable(true)
                .thumbnailUrl("https://example.com/image.jpg")
                .quantity(100)
                .publisher(publisher)
                .totalRating(5L)
                .reviewCount(2L)
                .likeCount(0L)
                .build();
    }

    @Test
    @DisplayName("도서 단건 조회 성공")
    void getBookSuccess() {
        when(bookRepository.findById(1L)).thenReturn(java.util.Optional.of(book));

        BookDetailResponse result = bookService.getBookDetail(1L);
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("책 제목");
        assertThat(result.averageRating()).isEqualTo(2.5);
    }

    @Test
    @DisplayName("전체 도서 조회 성공")
    void getBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(page);
        PageResponse<BookPreviewResponse> response = bookService.getBooks(PageRequest.of(0, 10));
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().title()).isEqualTo("책 제목");
    }

    @Test
    @DisplayName("Top5 베스트셀러 조회 성공")
    void getTop5BestSellerBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findSortedBestSellerBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(page);
        Pageable pageable = PageRequest.of(0, 5);
        PageResponse<BookPreviewResponse> result = bookService.getBestSellerBooks(SortType.REVIEW, pageable);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().title()).isEqualTo("책 제목");
    }

    @Test
    @DisplayName("Top5 추천도서 조회 성공")
    void getTop5RecommendedBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findSortedRecommendedBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(page);
        Pageable pageable = PageRequest.of(0, 5);
        PageResponse<BookPreviewResponse> result = bookService.getAllRecommendedBooks(SortType.REVIEW, pageable);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("책 제목");
    }

    @Test
    @DisplayName("Top5 신간 도서 조회 성공")
    void getTop5NewBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findSortedNewBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(page);
        Pageable pageable = PageRequest.of(0, 5);
        PageResponse<BookPreviewResponse> result = bookService.getAllNewBooks(SortType.REVIEW, pageable);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().title()).isEqualTo("책 제목");
    }

    @Test
    @DisplayName("전체 베스트셀러 조회 성공")
    void getAllBestSellerBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findSortedBestSellerBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(page);
        PageResponse<BookPreviewResponse> result = bookService.getBestSellerBooks(SortType.REVIEW,
                PageRequest.of(0, 5));
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().title()).isEqualTo("책 제목");
        assertThat(result.content().getFirst().reviewCount()).isEqualTo(2L);
        assertThat(result.content().getFirst().likeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("전체 신간 도서 조회 성공")
    void getAllNewBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findSortedNewBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(page);
        PageResponse<BookPreviewResponse> result = bookService.getAllNewBooks(SortType.REVIEW, PageRequest.of(0, 5));
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().title()).isEqualTo("책 제목");
        assertThat(result.content().getFirst().reviewCount()).isEqualTo(2L);
        assertThat(result.content().getFirst().likeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("전체 추천 도서 조회 성공")
    void getAllRecommendedBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findSortedRecommendedBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(page);
        PageResponse<BookPreviewResponse> result = bookService.getAllRecommendedBooks(SortType.REVIEW,
                PageRequest.of(0, 5));
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().title()).isEqualTo("책 제목");
        assertThat(result.content().getFirst().reviewCount()).isEqualTo(2L);
        assertThat(result.content().getFirst().likeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("관리자 도서 목록 조회 성공")
    void getAdminBooksSuccess() {
        Page<Book> page = new PageImpl<>(List.of(book));
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(page);

        PageResponse<?> result = bookService.getAdminBooks(PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst()).hasFieldOrPropertyWithValue("title", "책 제목");
    }

    @Test
    @DisplayName("도서 등록 실패 - 중복 ISBN")
    void createBookFail_duplicateIsbn() {
        when(bookRepository.existsByIsbn("1234567890123")).thenReturn(true);

        BookCreateRequest request = new BookCreateRequest(
            "1234567890123", "제목", null, "설명",
            LocalDate.now(), 10000, 9000, 10, BookStatus.AVAILABLE,
            true, "출판사", List.of(1L), List.of(new BookAuthorDto("홍길동", "지은이")), List.of("태그")
        );

        MultipartFile dummyFile = null;

        org.junit.jupiter.api.Assertions.assertThrows(
            DuplicateIsbnException.class,
            () -> bookService.createBook(request, dummyFile)
        );
    }

    @Test
    @DisplayName("도서 상세 조회 실패 - 존재하지 않는 도서")
    void getBookDetailFail_notFound() {
        when(bookRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            BookNotFoundException.class,
            () -> bookService.getBookDetail(999L)
        );
    }

    @Test
    @DisplayName("도서 삭제 실패 - 존재하지 않는 도서")
    void deleteBookFail_notFound() {
        when(bookRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            BookNotFoundException.class,
            () -> bookService.deleteBook(999L)
        );
    }
}
