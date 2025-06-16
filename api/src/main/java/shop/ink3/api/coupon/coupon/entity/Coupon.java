package shop.ink3.api.coupon.coupon.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import shop.ink3.api.book.book.entity.Book;
import shop.ink3.api.book.category.entity.Category;
import shop.ink3.api.coupon.bookCoupon.entity.BookCoupon;
import shop.ink3.api.coupon.categoryCoupon.entity.CategoryCoupon;
import shop.ink3.api.coupon.policy.entity.CouponPolicy;
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "coupons")
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDateTime issuableFrom;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean isActive;

    private LocalDateTime createdAt;


    @Builder.Default
    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookCoupon> bookCoupons = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CategoryCoupon> categoryCoupons = new HashSet<>();


    public void addBookCoupon(List<Book> bookList) {
        for(Book book : bookList) {
            BookCoupon bookCoupon = new BookCoupon(this, book);
            this.bookCoupons.add(bookCoupon);
        }
    }

    public void addCategoryCoupon(List<Category> categoryList) {
        for(Category category : categoryList) {
            CategoryCoupon categoryCoupon = new CategoryCoupon(this, category);
            this.categoryCoupons.add(categoryCoupon);
        }
    }

    public void update(
            CouponPolicy newPolicy,
            String newName,
            LocalDateTime newIssuableFrom,
            LocalDateTime newExpiresAt,
            boolean isActive,
            LocalDateTime newCreatedAt,
            List<Book> newBookList,
            List<Category> newCategoryList
    ) {
        // 1) 정책과 기본 필드
        this.couponPolicy = newPolicy;
        this.name = newName;
        this.issuableFrom = newIssuableFrom;
        this.expiresAt = newExpiresAt;
        this.isActive = isActive;
        this.createdAt = newCreatedAt;

        // 2) BookCoupon 관계 리셋
        this.bookCoupons.clear();
        if (!newBookList.isEmpty()) {
            for (Book book : newBookList) {
                BookCoupon bc = new BookCoupon(this, book);
                this.bookCoupons.add(bc);
            }
        }

        // 3) CategoryCoupon 관계 리셋
        this.categoryCoupons.clear();
        if (!newCategoryList.isEmpty()) {
            for (Category category : newCategoryList) {
                CategoryCoupon cc = new CategoryCoupon(this, category);
                this.categoryCoupons.add(cc);
            }
        }
    }

}
