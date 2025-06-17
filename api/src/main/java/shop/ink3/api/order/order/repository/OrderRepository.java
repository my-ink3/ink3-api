package shop.ink3.api.order.order.repository;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import shop.ink3.api.order.order.dto.OrderWithDetailsResponse;
import shop.ink3.api.order.order.entity.Order;
import shop.ink3.api.order.order.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 전체적으로 querydsl을 프로젝트에서 사용안함 + 쿼리 성능 향상이 목표이기 때문에 native query 사용
    @Query(
            value = """
        SELECT\s
            o.id AS id,
            o.order_uuid AS orderUUID,
            o.status AS status,
            o.ordered_at AS orderedAt,
            o.orderer_name AS ordererName,
            o.orderer_phone AS ordererPhone,
            p.payment_amount AS paymentAmount,
            b.title AS representativeBookName,
            b.thumbnail_url AS representativeThumbnailUrl,
            (
                SELECT COUNT(DISTINCT ob2.book_id)
                FROM order_books ob2
                WHERE ob2.order_id = o.id
            ) AS bookTypeCount,
            
            ob.id AS orderBookId,
                b.id AS bookId,
                EXISTS (
                    SELECT 1
                    FROM reviews r
                    WHERE r.order_book_id = ob.id
                ) AS hasReview
            
        FROM orders o
        JOIN payments p ON p.order_id = o.id
        JOIN order_books ob ON ob.id = (
            SELECT MIN(ob_sub.id)
            FROM order_books ob_sub
            WHERE ob_sub.order_id = o.id
        )
        JOIN books b ON ob.book_id = b.id
        WHERE o.user_id = :userId
        ORDER BY o.ordered_at DESC
       \s""",
            countQuery = "SELECT COUNT(*) FROM orders o WHERE o.user_id = :userId",
            nativeQuery = true
    )
    Page<OrderWithDetailsResponse> findAllByUserId(long userId, Pageable pageable);


    Page<Order> findAll(Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findAllByUserIdAndStatus(long userId, OrderStatus status, Pageable pageable);
    Page<Order> findAllByOrderedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<Order> findAllByUserIdAndOrderedAtBetween(long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
