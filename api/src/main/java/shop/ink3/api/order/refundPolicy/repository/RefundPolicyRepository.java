package shop.ink3.api.order.refundPolicy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.ink3.api.order.refundPolicy.entity.RefundPolicy;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {
    RefundPolicy findByIsAvailableTrue();
}
