package shop.ink3.api.book.book.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import shop.ink3.api.book.author.entity.Author;
import shop.ink3.api.book.author.repository.AuthorRepository;
import shop.ink3.api.book.book.dto.AdminBookResponse;
import shop.ink3.api.book.book.dto.BookAuthorDto;
import shop.ink3.api.book.book.dto.BookCreateRequest;
import shop.ink3.api.book.book.dto.BookDetailResponse;
import shop.ink3.api.book.book.dto.BookPreviewResponse;
import shop.ink3.api.book.book.dto.BookRegisterRequest;
import shop.ink3.api.book.book.dto.BookUpdateRequest;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.enums.SortType;
import shop.ink3.api.book.book.exception.BookNotFoundException;
import shop.ink3.api.book.book.exception.DuplicateIsbnException;
import shop.ink3.api.book.book.exception.InvalidCategorySelectionException;
import shop.ink3.api.book.book.external.aladin.dto.AladinBookResponse;
import shop.ink3.api.book.book.repository.BookRepository;
import shop.ink3.api.book.bookauthor.entity.BookAuthor;
import shop.ink3.api.book.bookauthor.repository.BookAuthorRepository;
import shop.ink3.api.book.bookcategory.entity.BookCategory;
import shop.ink3.api.book.bookcategory.repository.BookCategoryRepository;
import shop.ink3.api.book.booktag.entity.BookTag;
import shop.ink3.api.book.booktag.repository.BookTagRepository;
import shop.ink3.api.book.category.dto.CategoryFlatDto;
import shop.ink3.api.book.category.entity.Category;
import shop.ink3.api.book.category.exception.CategoryNotFoundException;
import shop.ink3.api.book.category.repository.CategoryRepository;
import shop.ink3.api.book.category.service.CategoryService;
import shop.ink3.api.book.publisher.entity.Publisher;
import shop.ink3.api.book.publisher.repository.PublisherRepository;
import shop.ink3.api.book.tag.entity.Tag;
import shop.ink3.api.book.tag.repository.TagRepository;
import shop.ink3.api.common.dto.PageResponse;
import shop.ink3.api.common.uploader.MinioService;

@Transactional
@RequiredArgsConstructor
@Service
public class BookService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final MinioService minioService;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final CategoryService categoryService;

    @Value("${minio.book-bucket}")
    private String bucket;

    @Transactional(readOnly = true)
    public BookDetailResponse getBookDetail(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        List<List<CategoryFlatDto>> categories = getBookCategories(bookId);
        List<BookAuthorDto> authors = getBookAuthors(bookId);
        List<String> tags = getBookTags(bookId);
        return BookDetailResponse.from(
                book,
                getThumbnailUrl(book),
                categories,
                authors,
                tags
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<BookPreviewResponse> getBooks(Pageable pageable) {
        Page<Book> books = bookRepository.findAll(pageable);
        Page<BookPreviewResponse> response = mapToBookPreviewResponse(books);
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminBookResponse> getAdminBooks(Pageable pageable) {
        Page<Book> books = bookRepository.findAll(pageable);
        return PageResponse.from(books.map(b -> AdminBookResponse.from(b, getThumbnailUrl(b))));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "books:best-sellers", key = "#sortType.name()", condition = "#pageable.pageNumber == 0")
    public PageResponse<BookPreviewResponse> getBestSellerBooks(SortType sortType, Pageable pageable) {
        Page<Book> bestSellerBooks = bookRepository.findSortedBestSellerBooks(sortType, pageable);
        Page<BookPreviewResponse> response = mapToBookPreviewResponse(bestSellerBooks);
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "books:new", key = "#sortType.name()", condition = "#pageable.pageNumber == 0")
    public PageResponse<BookPreviewResponse> getAllNewBooks(SortType sortType, Pageable pageable) {
        Page<Book> bestRecommendedBooks = bookRepository.findSortedNewBooks(sortType, pageable);
        Page<BookPreviewResponse> response = mapToBookPreviewResponse(bestRecommendedBooks);
        return PageResponse.from(response);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "books:recommended", key = "#sortType.name()", condition = "#pageable.pageNumber == 0")
    public PageResponse<BookPreviewResponse> getAllRecommendedBooks(SortType sortType, Pageable pageable) {
        Page<Book> bestRecommendedBooks = bookRepository.findSortedRecommendedBooks(sortType, pageable);
        Page<BookPreviewResponse> response = mapToBookPreviewResponse(bestRecommendedBooks);
        return PageResponse.from(response);
    }

    public BookDetailResponse createBook(BookCreateRequest request, MultipartFile coverImage) {

        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new DuplicateIsbnException(request.isbn());
        }

        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new InvalidCategorySelectionException("최소 한 개 이상의 카테고리를 선택해야 합니다.");
        }

        if (request.categoryIds().size() > 10) {
            throw new InvalidCategorySelectionException("카테고리는 최대 10개까지만 선택할 수 있습니다.");
        }

        Publisher publisher = publisherRepository.findByName(request.publisher())
                .orElseGet(() -> publisherRepository.save(Publisher.builder().name(request.publisher()).build()));

        Book book = Book.builder()
                .isbn(request.isbn())
                .title(request.title())
                .contents(request.contents())
                .description(request.description())
                .publisher(publisher)
                .publishedAt(request.publishedAt())
                .originalPrice(request.originalPrice())
                .salePrice(request.salePrice())
                .quantity(request.quantity())
                .isPackable(request.isPackable())
                .totalRating(0L)
                .reviewCount(0L)
                .likeCount(0L)
                .thumbnailUrl(minioService.upload(coverImage, bucket))
                .status(request.status())
                .build();

        book = bookRepository.save(book);

        for (Category category : categoryRepository.findAllById(request.categoryIds())) {
            addCategoryToBook(book.getId(), category.getId());
        }

        for (BookAuthorDto author : request.authors()) {
            addAuthorToBook(book.getId(), author);
        }

        for (String tag : request.tags()) {
            addTagToBook(book.getId(), tag);
        }

        List<List<CategoryFlatDto>> categories = getBookCategories(book.getId());

        return BookDetailResponse.from(
                book,
                minioService.getPresignedUrl(book.getThumbnailUrl(), bucket),
                categories,
                request.authors(),
                request.tags()
        );
    }

    public BookDetailResponse updateBook(Long bookId, BookUpdateRequest request, MultipartFile coverImage) {

        if (request.categoryIds() == null || request.categoryIds().isEmpty()) {
            throw new InvalidCategorySelectionException("최소 한 개 이상의 카테고리를 선택해야 합니다.");
        }

        if (request.categoryIds().size() > 10) {
            throw new InvalidCategorySelectionException("카테고리는 최대 10개까지만 선택할 수 있습니다.");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        // 수정 시 중복 ISBN 허용하지 않음, request로 동일한 ISBN이 입력되어도 같은 도서이므로 throw하지 않음
        if (bookRepository.existsByIsbn(request.isbn()) && !book.getIsbn().equals(request.isbn())) {
            throw new DuplicateIsbnException(request.isbn());
        }

        Publisher publisher = publisherRepository.findByName(request.publisher())
                .orElseGet(() -> publisherRepository.save(Publisher.builder().name(request.publisher()).build()));

        // 입력받은 이미지 파일이 없으면 기존 imageUrl로 유지
        String imageUrl = book.getThumbnailUrl();
        if (coverImage != null && !coverImage.isEmpty()) {
            minioService.delete(imageUrl, bucket);
            imageUrl = minioService.upload(coverImage, bucket);
        }

        book.updateBook(
                request.isbn(),
                request.title(),
                request.contents(),
                request.description(),
                request.publishedAt(),
                request.originalPrice(),
                request.salePrice(),
                request.quantity(),
                request.status(),
                request.isPackable(),
                imageUrl,
                publisher
        );

        bookCategoryRepository.deleteAllByBookId(book.getId());
        bookAuthorRepository.deleteAllByBookId(book.getId());
        bookTagRepository.deleteAllByBookId(book.getId());

        categoryRepository.findAllById(request.categoryIds())
                .forEach(category -> addCategoryToBook(book.getId(), category.getId()));
        request.authors().forEach(author -> addAuthorToBook(book.getId(), author));
        request.tags().forEach(tag -> addTagToBook(book.getId(), tag));

        List<List<CategoryFlatDto>> categories = getBookCategories(book.getId());

        return BookDetailResponse.from(
                book,
                getThumbnailUrl(book),
                categories,
                request.authors(),
                request.tags()
        );
    }

    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(
                () -> new BookNotFoundException(bookId));
        book.delete();
    }

    public List<List<CategoryFlatDto>> getBookCategories(long bookId) {
        List<List<CategoryFlatDto>> categories = new ArrayList<>();
        bookCategoryRepository.findAllByBookId(bookId).stream()
                .map(bc -> categoryService.getAllAncestors(bc.getCategory().getId()))
                .forEach(categories::add);
        return categories;
    }

    public void addCategoryToBook(Long bookId, Long categoryId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        if (category.getDepth() < 1) {
            return;
        }
        bookCategoryRepository.save(new BookCategory(book, category));
    }

    public List<BookAuthorDto> getBookAuthors(long bookId) {
        return bookAuthorRepository.findAllByBookId(bookId).stream()
                .map(ba -> new BookAuthorDto(ba.getAuthor().getName(), ba.getRole()))
                .toList();
    }

    public void addAuthorToBook(Long bookId, BookAuthorDto dto) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
        Author author = authorRepository.findByName(dto.name())
                .orElseGet(() -> authorRepository.save(Author.builder().name(dto.name()).build()));

        BookAuthor bookAuthor = new BookAuthor(book, author, dto.role());
        bookAuthorRepository.save(bookAuthor);
    }

    public List<String> getBookTags(long bookId) {
        return bookTagRepository.findAllByBookId(bookId).stream()
                .map(bt -> bt.getTag().getName())
                .toList();
    }

    public void addTagToBook(Long bookId, String tagName) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        Tag tag = tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
        BookTag bookTag = new BookTag(book, tag);
        bookTagRepository.save(bookTag);
    }

    // 알라딘 api + 자체적으로 조정할 내용 입력하여 도서 등록
    public BookDetailResponse registerBookByAladin(BookRegisterRequest request) {
        AladinBookResponse dto = request.aladinBookResponse();
        if (bookRepository.existsByIsbn(dto.isbn13())) {
            throw new DuplicateIsbnException(dto.isbn13());
        }

        Publisher publisher = publisherRepository.findByName(dto.publisher())
                .orElseGet(() -> publisherRepository.save(Publisher.builder().name(dto.publisher()).build()));

        Book book = Book.builder()
                .isbn(dto.isbn13())
                .title(dto.title())
                .contents(request.contents())
                .description(dto.description())
                .publisher(publisher)
                .publishedAt(LocalDate.parse(dto.pubDate()))
                .originalPrice(dto.priceStandard())
                .salePrice(request.priceSales())
                .quantity(request.quantity())
                .isPackable(request.isPackable())
                .totalRating(0L)
                .reviewCount(0L)
                .likeCount(0L)
                .thumbnailUrl(dto.cover())
                .status(request.status())
                .build();

        bookRepository.save(book); // 먼저 저장해서 ID 확보

        List<List<CategoryFlatDto>> categories = createCategoryHierarchy(dto.categoryName());
        for (List<CategoryFlatDto> path : categories) {
            CategoryFlatDto selectedCategory = path.getLast();
            addCategoryToBook(book.getId(), selectedCategory.id());
        }

        List<BookAuthorDto> authors = parseAuthors(dto.author());
        authors.forEach(author -> addAuthorToBook(book.getId(), author));

        if (request.tags() != null) {
            request.tags().forEach(tag -> addTagToBook(book.getId(), tag));
        }

        return BookDetailResponse.from(
                book,
                book.getThumbnailUrl(),
                categories,
                authors,
                request.tags()
        );
    }

    public List<BookAuthorDto> parseAuthors(String rawAuthors) {
        List<String> parts = Arrays.stream(rawAuthors.split(","))
                .map(String::trim)
                .toList();

        List<BookAuthorDto> result = new ArrayList<>();

        Pattern pattern = Pattern.compile("^(.*)\\(([^()]+)\\)$");
        String currentRole = null;
        for (int i = parts.size() - 1; i >= 0; i--) {
            String part = parts.get(i);
            Matcher matcher = pattern.matcher(part);

            String name;
            String role;

            if (matcher.matches()) {
                name = matcher.group(1).trim();
                currentRole = matcher.group(2).trim();
                role = currentRole;
            } else {
                name = part;
                role = (currentRole != null) ? currentRole : "지은이";
            }
            result.add(new BookAuthorDto(name, role));
        }
        return result;
    }

    public List<List<CategoryFlatDto>> createCategoryHierarchy(String categoryPath) {
        List<Category> categories = new ArrayList<>();
        String[] categoryNames = categoryPath.split(">");

        Arrays.stream(categoryNames)
                .map(c -> categoryRepository.findByName(c.strip())
                        .orElseGet(() -> categoryRepository.save(Category.builder().name(c).path("").build())))
                .forEach(categories::add);

        for (int i = 1; i < categories.size(); i++) {
            categories.get(i).updateParent(categories.get(i - 1));
            categories.get(i).updatePath(categories.get(i - 1).getPath() + "/" + categories.get(i - 1).getId());
        }

        return List.of(categories.stream().map(CategoryFlatDto::from).toList());
    }

    private Page<BookPreviewResponse> mapToBookPreviewResponse(Page<Book> books) {
        return books.map(book -> {
            List<String> authors = bookAuthorRepository.findAllByBookId(book.getId()).stream()
                    .map(ba -> "%s (%s)".formatted(ba.getAuthor().getName(), ba.getRole()))
                    .toList();
            return BookPreviewResponse.from(
                    book,
                    getThumbnailUrl(book),
                    authors
            );
        });
    }

    private String getThumbnailUrl(Book book) {
        return book.getThumbnailUrl().startsWith("https") ? book.getThumbnailUrl()
                : minioService.getPresignedUrl(book.getThumbnailUrl(), bucket);
    }
}
