package com.tresofertas.dto.response;

import com.tresofertas.model.Merchant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MerchantResponse {
    private Long id;
    private String name;
    private String slug;
    private String category;
    private String subCategory;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String whatsapp;
    private String phone;
    private String email;
    private String schedule;
    private String photoUrl;
    private Boolean verified;
    private Boolean active;
    private LocalDateTime createdAt;

    public static MerchantResponse from(Merchant m) {
        MerchantResponse r = new MerchantResponse();
        r.id = m.getId();
        r.name = m.getName();
        r.slug = m.getSlug();
        r.category = m.getCategory();
        r.subCategory = m.getSubCategory();
        r.address = m.getAddress();
        r.lat = m.getLat();
        r.lng = m.getLng();
        r.whatsapp = m.getWhatsapp();
        r.phone = m.getPhone();
        r.email = m.getEmail();
        r.schedule = m.getSchedule();
        r.photoUrl = m.getPhotoUrl();
        r.verified = m.getVerified();
        r.active = m.getActive();
        r.createdAt = m.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getCategory() { return category; }
    public String getSubCategory() { return subCategory; }
    public String getAddress() { return address; }
    public BigDecimal getLat() { return lat; }
    public BigDecimal getLng() { return lng; }
    public String getWhatsapp() { return whatsapp; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getSchedule() { return schedule; }
    public String getPhotoUrl() { return photoUrl; }
    public Boolean getVerified() { return verified; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
