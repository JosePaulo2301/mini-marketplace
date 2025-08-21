package io.github.minimarketplace.order.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ORderDTO(
    UUID id,
    String customerEmail,
    BigDecimal total,
    String status,
    List<OrderItemDTO> items
) {}