package shop.ink3.api.book.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import shop.ink3.api.book.book.dto.AdminBookResponse;
import shop.ink3.api.book.book.dto.BookCreateRequest;
import shop.ink3.api.book.book.dto.BookDetailResponse;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.book.book.dto.BookUpdateRequest;
import shop.ink3.api.book.book.enums.SortType;
import shop.ink3.api.book.book.service.BookService;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.elastic.repository.BookSearchRedisRepository;

@RequestMapping("/books")
@RequiredArgsConstructor
@RestController
public class BookController {
    private final BookService bookService;
    private final ObjectMapper objectMapper;
    private final BookSearchRedisRepository bookSearchRedisRepository;

    @GetMapping("/{bookId}")
    public ResponseEntity<CommonResponse<BookDetailResponse>> getBookByIdWithParentCategory(@PathVariable Long bookId) {
        return ResponseEntity.ok(CommonResponse.success(bookService.getBookDetail(bookId)));
    }

    // 전체 도서 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getBooks(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(bookService.getBooks(pageable)));
    }

    @GetMapping("/admin")
    public ResponseEntity<CommonResponse<PageResponse<AdminBookResponse>>> getBooksByAdmin(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(bookService.getAdminBooks(pageable)));
    }

    @GetMapping("/bestseller")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getTop5BestsellerBooks() {
        return ResponseEntity.ok(
                CommonResponse.success(bookService.getBestSellerBooks(SortType.REVIEW, PageRequest.of(0, 5))));
    }

    @GetMapping("/bestseller-all")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getAllBestsellerBooks(
            @RequestParam(defaultValue = "REVIEW") SortType sortType, Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(bookService.getBestSellerBooks(sortType, pageable)));
    }

    @GetMapping("/new")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getTop5NewBooks() {
        return ResponseEntity.ok(
                CommonResponse.success(bookService.getAllNewBooks(SortType.REVIEW, PageRequest.of(0, 5))));
    }

    @GetMapping("/new-all")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getAllNewBooks(
            @RequestParam(defaultValue = "REVIEW") SortType sortType, Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(bookService.getAllNewBooks(sortType, pageable)));
    }

    @GetMapping("/recommend")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getTop5RecommendedBooks() {
        return ResponseEntity.ok(
                CommonResponse.success(bookService.getAllRecommendedBooks(SortType.REVIEW, PageRequest.of(0, 5))));
    }

    @GetMapping("/recommend-all")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> getAllRecommendedBooks(
            @RequestParam(defaultValue = "REVIEW") SortType sortType, Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(bookService.getAllRecommendedBooks(sortType, pageable)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<BookDetailResponse>> createBook(
            @RequestPart("book") String bookCreateRequestJson,
            @RequestPart("coverImage") MultipartFile coverImage
    ) {
        try {
            BookCreateRequest bookCreateRequest = objectMapper.readValue(
                    bookCreateRequestJson,
                    BookCreateRequest.class
            );
            BookDetailResponse response = bookService.createBook(bookCreateRequest, coverImage);
            return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CommonResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), null));
        }
    }

    @PostMapping("/{bookId}/view")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long bookId) {
        bookSearchRedisRepository.incrementViewCount(bookId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{bookId}/search")
    public ResponseEntity<Void> increaseSearchCount(@PathVariable Long bookId) {
        bookSearchRedisRepository.incrementSearchCount(bookId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<CommonResponse<BookDetailResponse>> updateBook(
            @PathVariable Long bookId,
            @RequestPart("book") String bookUpdateRequestJson,
            @RequestPart("coverImage") MultipartFile coverImage
    ) {
        try {
            BookUpdateRequest bookUpdateRequest = objectMapper.readValue(bookUpdateRequestJson,
                    BookUpdateRequest.class);
            BookDetailResponse response = bookService.updateBook(bookId, bookUpdateRequest, coverImage);
            return ResponseEntity.ok(CommonResponse.update(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CommonResponse.error(HttpStatus.BAD_REQUEST, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
