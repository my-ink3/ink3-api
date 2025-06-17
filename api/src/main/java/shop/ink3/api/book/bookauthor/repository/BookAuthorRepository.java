package shop.ink3.api.book.bookauthor.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.bookauthor.entity.BookAuthor;

public interface BookAuthorRepository extends CrudRepository<BookAuthor, Long> {
    @EntityGraph(attributePaths = "author")
    List<BookAuthor> findAllByBookId(Long bookId);

    @Transactional
    @Modifying
    @Query("DELETE FROM BookAuthor ba WHERE ba.book.id = :bookId")
    void deleteAllByBookId(@Param("bookId") Long bookId);
}
