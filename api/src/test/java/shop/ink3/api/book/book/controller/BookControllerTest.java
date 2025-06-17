package shop.ink3.api.book.book.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import shop.ink3.api.book.book.dto.AdminBookResponse;
import shop.ink3.api.book.book.dto.BookAuthorDto;
import shop.ink3.api.book.book.dto.BookCreateRequest;
import shop.ink3.api.book.book.dto.BookDetailResponse;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.book.book.dto.BookUpdateRequest;
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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    @Test
    @DisplayName("도서 상세 조회")
    void getBookByIdWithParentCategory() throws Exception {
        when(bookService.getBookDetail(1L)).thenReturn(bookDetailResponse);

        mockMvc.perform(get("/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("책 제목 (상세)"));
    }

    @Test
    @DisplayName("도서 등록")
    void createBook() throws Exception {
        String json = objectMapper.writeValueAsString(
            new BookCreateRequest(
                "9781234567890",
                "테스트 도서 제목",
                "목차 예시",
                "도서 설명 예시입니다.",
                LocalDate.of(2024, 1, 1),
                15000,
                12000,
                100,
                BookStatus.AVAILABLE,
                true,
                "출판사 예시",
                List.of(1L, 2L),
                List.of(new BookAuthorDto("홍길동", "저자")),
                List.of("추천", "베스트셀러")
            )
        );

        MockMultipartFile bookJson = new MockMultipartFile("book", "", "application/json", json.getBytes());
        MockMultipartFile image = new MockMultipartFile("coverImage", "image.jpg", "image/jpeg", "image".getBytes());

        when(bookService.createBook(any(), any())).thenReturn(bookDetailResponse);

        mockMvc.perform(multipart("/books")
                .file(bookJson)
                .file(image)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.title").value("책 제목 (상세)"));
    }

    @Test
    @DisplayName("도서 수정")
    void updateBook() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = objectMapper.writeValueAsString(
            new BookUpdateRequest(
                "9781234567890",
                "수정된 도서 제목",
                "수정된 목차입니다.",
                "이것은 수정된 도서 설명입니다.",
                LocalDate.of(2024, 6, 1),
                18000,
                15000,
                50,
                BookStatus.AVAILABLE,
                false,
                "https://example.com/updated-thumbnail.jpg",
                "수정된 출판사",
                List.of(2L, 3L),
                List.of(new BookAuthorDto("이몽룡", "저자")),
                List.of("리뷰많음", "한정판")
            )
        );

        MockMultipartFile bookJson = new MockMultipartFile("book", "", "application/json", json.getBytes());
        MockMultipartFile image = new MockMultipartFile("coverImage", "image.jpg", "image/jpeg", "image".getBytes());

        when(bookService.updateBook(eq(1L), any(), any())).thenReturn(bookDetailResponse);

        mockMvc.perform(multipart("/books/1")
                .file(bookJson)
                .file(image)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                })
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("책 제목 (상세)"));
    }

    @Test
    @DisplayName("도서 삭제")
    void deleteBook() throws Exception {
        mockMvc.perform(delete("/books/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("도서 조회수 증가")
    void increaseViewCount() throws Exception {
        mockMvc.perform(post("/books/1/view"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("도서 검색수 증가")
    void increaseSearchCount() throws Exception {
        mockMvc.perform(post("/books/1/search"))
            .andExpect(status().isOk());
    }
}
