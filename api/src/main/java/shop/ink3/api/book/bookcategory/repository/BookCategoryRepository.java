package shop.ink3.api.book.bookcategory.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shop.ink3.api.book.bookcategory.entity.BookCategory;

@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {
    @EntityGraph(attributePaths = "category")
    List<BookCategory> findAllByBookId(long bookId);

    @Transactional
    @Modifying
    @Query("DELETE FROM BookCategory bc WHERE bc.book.id = :bookId")
    void deleteAllByBookId(@Param("bookId") long bookId);
}
