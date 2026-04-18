package com.tresofertas.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateOfferRequest {
    private String description;
    private BigDecimal price;
    private Boolean untilStockOut;
    private LocalDateTime expiresAt;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Boolean getUntilStockOut() { return untilStockOut; }
    public void setUntilStockOut(Boolean untilStockOut) { this.untilStockOut = untilStockOut; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
