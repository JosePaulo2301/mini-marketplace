package io.github.minimarketplace.order.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.minimarketplace.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {}
