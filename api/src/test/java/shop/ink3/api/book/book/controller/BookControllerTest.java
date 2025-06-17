package shop.ink3.api.book.book.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import shop.ink3.api.book.book.dto.AdminBookResponse;
import shop.ink3.api.book.book.dto.BookAuthorDto;
import shop.ink3.api.book.book.dto.BookDetailResponse;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.book.book.entity.BookStatus;
import shop.ink3.api.book.book.enums.SortType;
import shop.ink3.api.book.book.service.BookService;
import shop.ink3.api.book.category.dto.CategoryFlatDto;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.elastic.repository.BookSearchRedisRepository;

@WebMvcTest(BookController.class)
class BookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private BookSearchRedisRepository bookSearchRedisRepository;

    private BookDetailResponse bookDetailResponse;
    private BookPreviewResponse bookPreviewResponse;
    private AdminBookResponse adminBookResponse;

    @BeforeEach
    void setUp() {
        CategoryFlatDto category1 = new CategoryFlatDto(
                1L, "국내도서", null, 0
        );
        CategoryFlatDto category2 = new CategoryFlatDto(
                3L, "한국소설", 1L, 1
        );
        List<CategoryFlatDto> categoryFlatDtos = List.of(category1, category2);
        List<List<CategoryFlatDto>> categories = List.of(categoryFlatDtos);

        BookAuthorDto author = new BookAuthorDto(
                "홍길동", "작가"
        );
        List<BookAuthorDto> authors = List.of(author);

        List<String> tags = List.of("베스트셀러");

        this.bookDetailResponse = new BookDetailResponse(
                1L,                           // id
                "1234567890123",                 // isbn
                "책 제목 (상세)",                 // title
                "책 목차",                        // contents
                "상세 설명",                      // description
                "출판사",                         // publisherName
                LocalDate.of(2024, 1, 1),       // publishedAt
                20000,                          // originalPrice
                18000,                          // salePrice
                10,                             // discountRate
                100,                            // quantity
                true,                           // isPackable
                "https://example.com/detail/image.jpg", // thumbnailUrl
                categories,                     // List<List<CategoryFlatDto>>
                authors,                        // List<BookAuthorDto>
                tags,                           // List<String>
                4.5,                            // ⭐ averageRating
                0L,                             // reviewCount
                0L,                             // likeCount
                BookStatus.AVAILABLE            // status
        );

        this.bookPreviewResponse = new BookPreviewResponse(
                2L, "책 제목 (프리뷰)", 20000, 18000, 10,
                "https://example.com/preview/image.jpg",
                List.of("홍길동 (작가)"),
                4.5,
                0L, 0L
        );

        this.adminBookResponse = new AdminBookResponse(
                1L,                           // id
                "1234567890123",                 // isbn
                "책 제목 (관리자)",                 // title
                "출판사",                         // publisher
                LocalDate.of(2024, 1, 1),       // publishedAt
                20000,                          // originalPrice
                18000,                          // salePrice
                10,                             // discountRate
                100,                            // quantity
                true,                           // isPackable
                4.5,                            // ⭐ averageRating
                "https://example.com/admin/image.jpg", // thumbnailUrl
                BookStatus.AVAILABLE            // status
        );
    }

    @Test
    @DisplayName("전체 도서 목록 조회")
    void getBooks() throws Exception {
        PageResponse<BookPreviewResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getBooks(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }

    @Test
    @DisplayName("관리자 도서 목록 조회")
    void getBooksByAdmin() throws Exception {
        PageResponse<AdminBookResponse> pageResponse = PageResponse.from(new PageImpl<>(List.of(adminBookResponse)));
        when(bookService.getAdminBooks(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/books/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (관리자)"));
    }

    @Test
    @DisplayName("Top5 베스트셀러 조회")
    void getTop5BestsellerBooks() throws Exception {
        PageResponse<BookPreviewResponse> response = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getBestSellerBooks(SortType.REVIEW, PageRequest.of(0, 5))).thenReturn(response);

        mockMvc.perform(get("/books/bestseller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }

    @Test
    @DisplayName("Top5 신간 조회")
    void getTop5NewBooks() throws Exception {
        PageResponse<BookPreviewResponse> response = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getAllNewBooks(SortType.REVIEW, PageRequest.of(0, 5))).thenReturn(response);

        mockMvc.perform(get("/books/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }

    @Test
    @DisplayName("Top5 추천도서 조회")
    void getTop5RecommendedBooks() throws Exception {
        PageResponse<BookPreviewResponse> response = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getAllRecommendedBooks(SortType.REVIEW, PageRequest.of(0, 5))).thenReturn(response);

        mockMvc.perform(get("/books/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }

    @Test
    @DisplayName("전체 베스트셀러 조회")
    void getAllBestsellerBooks() throws Exception {
        PageResponse<BookPreviewResponse> response = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getBestSellerBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/books/bestseller-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }

    @Test
    @DisplayName("전체 신간 조회")
    void getAllNewBooks() throws Exception {
        PageResponse<BookPreviewResponse> response = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getAllNewBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/books/new-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }

    @Test
    @DisplayName("전체 추천도서 조회")
    void getAllRecommendedBooks() throws Exception {
        PageResponse<BookPreviewResponse> response = PageResponse.from(new PageImpl<>(List.of(bookPreviewResponse)));
        when(bookService.getAllRecommendedBooks(eq(SortType.REVIEW), any(Pageable.class))).thenReturn(response);

        mockMvc.perform(get("/books/recommend-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("책 제목 (프리뷰)"));
    }
}
