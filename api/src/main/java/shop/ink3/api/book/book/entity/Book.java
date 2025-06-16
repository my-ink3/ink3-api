package shop.ink3.api.book.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.ink3.api.book.publisher.entity.Publisher;

@Builder
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String contents;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Publisher publisher;

    @Column(nullable = false)
    private LocalDate publishedAt;

    @Column(nullable = false)
    private Integer originalPrice;

    @Column(nullable = false)
    private Integer salePrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private boolean isPackable;

    @Column(nullable = false)
    private Long totalRating;

    @Column(nullable = false)
    private Long reviewCount;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status;

    public int getDiscountRate() {
        if (originalPrice <= 0 || salePrice >= originalPrice) {
            return 0;
        }
        return (int) Math.floor(100.0 * (originalPrice - salePrice) / originalPrice);
    }

    public Double getAverageRating() {
        if (reviewCount == 0) {
            return 0.0;
        }
        return totalRating / (double) reviewCount;
    }

    public void updateBook(
            String isbn,
            String title,
            String contents,
            String description,
            LocalDate publishedAt,
            Integer originalPrice,
            Integer salePrice,
            Integer quantity,
            BookStatus status,
            boolean isPackable,
            String thumbnailUrl,
            Publisher publisher
    ) {
        this.isbn = isbn;
        this.title = title;
        this.contents = contents;
        this.description = description;
        this.publishedAt = publishedAt;
        this.originalPrice = originalPrice;
        this.salePrice = salePrice;
        this.quantity = quantity;
        this.status = status;
        this.isPackable = isPackable;
        this.thumbnailUrl = thumbnailUrl;
        this.publisher = publisher;
    }

    public void addRating(int rating) {
        if (this.totalRating == null) this.totalRating = 0L;
        if (this.reviewCount == null) this.reviewCount = 0L;
        this.totalRating += rating;
        this.reviewCount++;
    }

    public void updateRating(int oldRating, int newRating) {
        this.totalRating = this.totalRating - oldRating + newRating;
    }

    public void updateReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void incrementLikeCount() {
        this.likeCount = (this.likeCount == null) ? 1 : this.likeCount + 1;
    }

    public void decrementLikeCount() {
        if (this.likeCount != null && this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // 주문 시 재고 확인 및 재고 수량 감소
    public void decreaseQuantity(int amount) {
        if (this.quantity < amount) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        this.quantity -= amount;
    }

    // 반품 시 재고 수량 증가
    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    public void delete() {
        this.status = BookStatus.DELETED;
    }
}
