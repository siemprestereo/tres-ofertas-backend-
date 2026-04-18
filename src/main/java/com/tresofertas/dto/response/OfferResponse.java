package com.tresofertas.dto.response;

import com.tresofertas.model.Offer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferResponse {
    private Long id;
    private Long merchantId;
    private String merchantName;
    private String merchantSlug;
    private String code;
    private String description;
    private BigDecimal price;
    private String photoUrl;
    private Boolean untilStockOut;
    private LocalDateTime expiresAt;
    private Boolean active;
    private LocalDateTime createdAt;

    public static OfferResponse from(Offer o) {
        OfferResponse r = new OfferResponse();
        r.id = o.getId();
        r.merchantId = o.getMerchant().getId();
        r.merchantName = o.getMerchant().getName();
        r.merchantSlug = o.getMerchant().getSlug();
        r.code = o.getCode();
        r.description = o.getDescription();
        r.price = o.getPrice();
        r.photoUrl = o.getPhotoUrl();
        r.untilStockOut = o.getUntilStockOut();
        r.expiresAt = o.getExpiresAt();
        r.active = o.getActive();
        r.createdAt = o.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getMerchantId() { return merchantId; }
    public String getMerchantName() { return merchantName; }
    public String getMerchantSlug() { return merchantSlug; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getPhotoUrl() { return photoUrl; }
    public Boolean getUntilStockOut() { return untilStockOut; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
