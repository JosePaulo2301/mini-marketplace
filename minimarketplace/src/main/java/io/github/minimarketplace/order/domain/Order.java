package io.github.minimarketplace.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity 
@Table(name = "orders")
public class Order {
    private UUID id;
    private String customerEmail;
    private BigDecimal total;
    private String status;
    private Instant createdAt;
    

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem>  items = new ArrayList<>();

    public static Order create(String email, List<OrderItem> items) {
        Order o = new Order();
        o.id = UUID.randomUUID();
        o.customerEmail = email;
        o.status = "CREATED";
        o.createdAt = Instant.now();
        o.items = items;
        o.items.forEach(i -> i.setOrder(o));
        o.total = items.stream()
        .map(i -> i.getUnitPrice().multiply(new BigDecimal(i.getQty())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return o;
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }


    
}
