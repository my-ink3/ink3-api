package shop.ink3.api.book.booktag.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.booktag.entity.BookTag;

public interface BookTagRepository extends JpaRepository<BookTag, Long> {
    @EntityGraph(attributePaths = "tag")
    List<BookTag> findAllByBookId(long bookId);

    @Transactional
    @Modifying
    @Query("DELETE FROM BookTag bt WHERE bt.book.id = :bookId")
    void deleteAllByBookId(@Param("bookId") Long bookId);
}
