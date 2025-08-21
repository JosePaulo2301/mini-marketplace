package io.github.minimarketplace.order.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;


@Transactional
@Query("update OutboxEvent e set e.published=true where e.id=:id")
public interface OutboxRepository extends JpaRepository<OutboxRepository, UUID> {
    void markPublished(@Param("id") UUID id);
}
