package shop.ink3.api.elastic.controller;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.common.config.ElasticsearchConfig;
import shop.ink3.api.common.dto.CommonResponse;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.elastic.model.BookSortOption;
import shop.ink3.api.elastic.service.BookSearchService;

@ConditionalOnBean(ElasticsearchConfig.class)
@RequiredArgsConstructor
@RestController
@RequestMapping("/search/books")
public class BookSearchController {
    private final BookSearchService bookSearchService;

    @GetMapping("/by-keyword")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> searchBooksByKeyword(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "POPULARITY") String sort
    ) throws IOException {
        return ResponseEntity.ok(CommonResponse.success(
                bookSearchService.searchBooksByKeyword(keyword, page, size, BookSortOption.valueOf(sort))
        ));
    }

    @GetMapping("/by-category")
    public ResponseEntity<CommonResponse<PageResponse<BookPreviewResponse>>> searchBooksByCategory(
            @RequestParam String category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "POPULARITY") String sort
    ) throws IOException {
        return ResponseEntity.ok(CommonResponse.success(
                bookSearchService.searchBooksByCategory(category, page, size, BookSortOption.valueOf(sort))
        ));
    }
}
