package shop.ink3.api.book.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import shop.ink3.api.book.booktag.entity.BookTag;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Length(max=20)
    private String name;

    @Builder.Default
    @OneToMany(mappedBy = "tag",
            orphanRemoval = true)
    private List<BookTag> bookTags = new ArrayList<>();

    public void addBookTag(BookTag bookTag) {
        this.bookTags.add(bookTag);
    }

    public void updateTagName(@NotBlank String name) {
        this.name = name;
    }
}
