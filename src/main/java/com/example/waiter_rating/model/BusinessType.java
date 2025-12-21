package com.example.waiter_rating.model;

public enum BusinessType {
    RESTAURANT("Restaurante"),
    CAFE("Cafetería"),
    BAR("Bar"),
    HOTEL("Hotel"),
    HAIR_SALON("Peluquería"),
    BEAUTY_SALON("Salón de Belleza"),
    REPAIR_SHOP("Taller de Reparaciones"),
    CONSTRUCTION_COMPANY("Empresa de Construcción"),
    CLEANING_COMPANY("Empresa de Limpieza"),
    ELECTRICAL_COMPANY("Empresa Eléctrica"),
    PLUMBING_COMPANY("Empresa de Plomería"),
    FREELANCE("Independiente"),
    OTHER("Otro");

    private final String displayName;

    BusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}