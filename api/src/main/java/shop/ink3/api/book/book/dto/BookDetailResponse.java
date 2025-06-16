package shop.ink3.api.book.book.dto;

import java.time.LocalDate;
import java.util.List;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.book.entity.BookStatus;
import shop.ink3.api.book.category.dto.CategoryFlatDto;

public record BookDetailResponse(
        Long id,
        String isbn,
        String title,
        String contents,
        String description,
        String publisherName,
        LocalDate publishedAt,
        Integer originalPrice,
        Integer salePrice,
        Integer discountRate,
        Integer quantity,
        Boolean isPackable,
        String thumbnailUrl,
        List<List<CategoryFlatDto>> categories,
        List<BookAuthorDto> authors,
        List<String> tags,
        Double averageRating,
        Long reviewCount,
        Long likeCount,
        BookStatus status
) {
    public static BookDetailResponse from(
            Book book,
            String thumbnailUrl,
            List<List<CategoryFlatDto>> categories,
            List<BookAuthorDto> authors,
            List<String> tags,
            Double averageRating,
            Long reviewCount
    ) {
        return new BookDetailResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getContents(),
                book.getDescription(),
                book.getPublisher().getName(),
                book.getPublishedAt(),
                book.getOriginalPrice(),
                book.getSalePrice(),
                book.getDiscountRate(),
                book.getQuantity(),
                book.isPackable(),
                thumbnailUrl,
                categories,
                authors,
                tags,
                averageRating,
                reviewCount,
                book.getLikeCount(),
                book.getStatus()
        );
    }
}
