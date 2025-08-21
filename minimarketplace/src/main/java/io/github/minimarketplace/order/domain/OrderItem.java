package io.github.minimarketplace.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.ManyToAny;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id  private UUID id = UUID.randomUUID();
    @ManyToAny @JoinColumn(name = "order_id")
    private String sku;
    private int qty;
    private BigDecimal unitPrice;
    private Order order;
    

    public void setOrder(Order order) {
        this.order = order;
    }
    
    public int getQty() {
        return qty;
    }
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}