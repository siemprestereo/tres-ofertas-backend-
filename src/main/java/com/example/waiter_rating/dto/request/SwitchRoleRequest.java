package com.example.waiter_rating.dto.request;

import com.example.waiter_rating.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SwitchRoleRequest {

    @NotNull(message = "El nuevo rol es requerido")
    private UserRole newRole;

    private List<String> professionTypes;
    private String professionalTitle;

    // legacy — por compatibilidad con clientes que aún envíen el campo simple
    private String professionType;

    public List<String> getEffectiveProfessionTypes() {
        if (professionTypes != null && !professionTypes.isEmpty()) return professionTypes;
        if (professionType != null && !professionType.isEmpty()) return List.of(professionType);
        return List.of();
    }
}
