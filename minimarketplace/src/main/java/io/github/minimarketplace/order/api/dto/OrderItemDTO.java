package io.github.minimarketplace.order.api.dto;

import java.math.BigDecimal;

public record OrderItemDTO (String sky, int qty, BigDecimal unitPrice) {}
