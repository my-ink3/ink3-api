package shop.ink3.api.book.booktag.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.ink3.api.book.booktag.entity.BookTag;

public interface BookTagRepository extends JpaRepository<BookTag, Long> {
    @EntityGraph(attributePaths = "tag")
    List<BookTag> findAllByBookId(long bookId);

    void deleteAllByBookId(long bookId);
}
