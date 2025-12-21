package com.example.waiter_rating.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@DiscriminatorValue("CLIENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client extends AppUser {

    // Los clientes solo califican, no tienen datos adicionales por ahora
    // Pero podemos agregar su historial de calificaciones

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Rating> ratingsGiven = new ArrayList<>();

    @Override
    public String getUserType() {
        return "CLIENT";
    }
}