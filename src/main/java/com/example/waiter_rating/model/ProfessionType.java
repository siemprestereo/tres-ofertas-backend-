package com.example.waiter_rating.model;

public enum ProfessionType {
    WAITER("Mesero/a"),
    ELECTRICIAN("Electricista"),
    PLUMBER("Plomero/a"),
    PAINTER("Pintor/a"),
    CARPENTER("Carpintero/a"),
    HAIRDRESSER("Peluquero/a"),
    MECHANIC("Mecánico/a"),
    CLEANER("Personal de Limpieza"),
    CHEF("Chef/Cocinero"),
    BARTENDER("Bartender"),
    CONSTRUCTION_WORKER("Obrero de Construcción"),
    GARDENER("Jardinero/a"),
    OTHER("Otro");

    private final String displayName;

    ProfessionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}