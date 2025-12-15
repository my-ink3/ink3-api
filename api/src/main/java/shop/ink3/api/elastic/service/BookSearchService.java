package shop.ink3.api.elastic.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.book.dto.BookDetailResponse;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.book.bookauthor.repository.BookAuthorRepository;
import shop.ink3.api.common.config.ElasticsearchConfig;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.common.uploader.MinioService;
import shop.ink3.api.elastic.model.BookDocument;
import shop.ink3.api.elastic.model.BookSortOption;
import shop.ink3.api.elastic.repository.BookSearchRedisRepository;

@ConditionalOnBean(ElasticsearchConfig.class)
@Slf4j
@RequiredArgsConstructor
@Service
public class BookSearchService {
    @Value("${elasticsearch.index}")
    private String index;

    @Value("${minio.book-bucket}")
    private String bucket;

    private final ElasticsearchClient client;
    private final BookSearchRedisRepository bookSearchRedisRepository;
    private final BookRepository bookRepository;
    private final MinioService minioService;
    private final BookAuthorRepository bookAuthorRepository;

    public void indexBook(BookDocument bookDocument) {
        try {
            client.index(i -> i
                    .index(index)
                    .id(bookDocument.getId().toString())
                    .document(bookDocument)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BookDocument getBook(long bookId) {
        try {
            GetResponse<BookDocument> response = client.get(
                    g -> g.index(index).id(String.valueOf(bookId)),
                    BookDocument.class
            );
            return response.found() ? response.source() : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<BookPreviewResponse> searchBooksByKeyword(
            String keyword,
            int page,
            int size,
            BookSortOption sortOption
    ) throws IOException {
        BookSortOption safeSortOption = sortOption == null ? BookSortOption.POPULARITY : sortOption;

        SearchResponse<BookDocument> response = client.search(search -> search
                        .index(index)
                        .from(page * size)
                        .size(size)
                        .query(q -> q.
                                bool(b -> {
                                            b.should(sh -> sh.match(m -> m.field("title").query(keyword).boost(100f)));
                                            b.should(sh -> sh.match(m -> m.field("description").query(keyword).boost(10f)));
                                            b.should(sh -> sh.match(m -> m.field("tags").query(keyword).boost(50f)));
                                            b.should(sh -> sh.match(m -> m.field("authors").query(keyword).boost(50f)));

                                            if (safeSortOption == BookSortOption.RATING) {
                                                b.filter(f -> f.range(r -> r.number(n -> n
                                                        .field("reviewCount")
                                                        .gte(100.0)
                                                )));
                                            }

                                            return b;
                                        }
                                )
                        )
                        .sort(s -> s.field(f -> f.field(safeSortOption.getSortField()).order(safeSortOption.getSortOrder()))),
                BookDocument.class
        );
        return PageResponse.from(wrapToPage(response, page, size));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookPreviewResponse> searchBooksByCategory(
            String category,
            int page,
            int size,
            BookSortOption sortOption
    ) throws IOException {
        BookSortOption safeSortOption = sortOption == null ? BookSortOption.POPULARITY : sortOption;

        SearchResponse<BookDocument> response = client.search(search -> search
                        .index(index)
                        .from(page * size)
                        .size(size)
                        .query(q -> q
                            .bool(b -> b.filter(
                                f -> f.term(t -> t.field("categories.keyword").value(category))
                            ))
                        )
                        .sort(s -> s.field(f -> f.field(safeSortOption.getSortField()).order(safeSortOption.getSortOrder()))),
                BookDocument.class
        );
        return PageResponse.from(wrapToPage(response, page, size));
    }

    public void updateBook(BookDetailResponse bookDetailResponse) {
        BookDocument bookDocument = getBook(bookDetailResponse.id());
        bookDocument.updateBookDocument(bookDetailResponse);
        indexBook(bookDocument);
    }

    public void updateRatingAndReviewCount(long bookId, double rating, long reviewCount) {
        BookDocument bookDocument = getBook(bookId);
        bookDocument.updateRating(rating);
        bookDocument.updateReviewCount(reviewCount);
        indexBook(bookDocument);
    }

    public void updateRating(long bookId, double rating) {
        BookDocument bookDocument = getBook(bookId);
        bookDocument.updateRating(rating);
        indexBook(bookDocument);
    }

    @Scheduled(fixedRate = 600_000)
    public void updateViewAndSearchCount() {
        Map<Object, Object> viewCounts = bookSearchRedisRepository.getAllViewCounts();
        Map<Object, Object> searchCounts = bookSearchRedisRepository.getAllSearchCounts();
        bookSearchRedisRepository.clearAllCounts();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(viewCounts.keySet().stream().map(Object::toString).toList());
        allKeys.addAll(searchCounts.keySet().stream().map(Object::toString).toList());

        for (String key : allKeys) {
            long bookId = Long.parseLong(key);
            BookDocument bookDocument = getBook(bookId);
            if (bookDocument == null) {
                continue;
            }

            if (viewCounts.containsKey(key)) {
                int viewCount = Integer.parseInt(viewCounts.get(key).toString());
                bookDocument.updateViewCount(viewCount);
            }

            if (searchCounts.containsKey(key)) {
                int searchCount = Integer.parseInt(searchCounts.get(key).toString());
                bookDocument.updateSearchCount(searchCount);
            }

            indexBook(bookDocument);
        }
    }

    public void deleteBook(long bookId) {
        try {
            client.delete(d -> d.index(index).id(String.valueOf(bookId)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PageImpl<BookPreviewResponse> wrapToPage(SearchResponse<BookDocument> response, int page, int size) {
        List<Long> bookIds = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(BookDocument::getId)
                .toList();

        if (bookIds.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        List<Book> books = bookRepository.findAllById(bookIds);

        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));

        List<BookPreviewResponse> content = bookIds.stream()
                .map(bookMap::get)
                .filter(Objects::nonNull)
                .map(book -> BookPreviewResponse.from(
                        book,
                        getThumbnailUrl(book),
                        bookAuthorRepository.findAllByBookId(book.getId()).stream()
                                .map(ba -> "%s (%s)".formatted(ba.getAuthor().getName(), ba.getRole()))
                                .toList()
                ))
                .toList();

        long total = response.hits().total() != null ? response.hits().total().value() : content.size();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private String getThumbnailUrl(Book book) {
        return book.getThumbnailUrl().startsWith("https") ? book.getThumbnailUrl()
                : minioService.getPresignedUrl(book.getThumbnailUrl(), bucket);
    }
}
