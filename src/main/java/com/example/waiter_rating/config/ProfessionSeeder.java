package com.example.waiter_rating.config;

import com.example.waiter_rating.model.Profession;
import com.example.waiter_rating.repository.ProfessionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProfessionSeeder implements ApplicationRunner {

    private final ProfessionRepo professionRepo;

    private record Seed(String code, String displayName, String category) {}

    private static final List<Seed> INITIAL_PROFESSIONS = List.of(
        new Seed("WAITER",                      "Mesero/a",                  "Gastronom\u00eda"),
        new Seed("CHEF",                         "Chef/Cocinero",              "Gastronom\u00eda"),
        new Seed("BARTENDER",                    "Bartender",                  "Gastronom\u00eda"),
        new Seed("BARISTA",                      "Barista",                    "Gastronom\u00eda"),
        new Seed("ELECTRICIAN",                  "Electricista",               "Hogar y mantenimiento"),
        new Seed("PLUMBER",                      "Plomero/a",                  "Hogar y mantenimiento"),
        new Seed("PAINTER",                      "Pintor/a",                   "Hogar y mantenimiento"),
        new Seed("CARPENTER",                    "Carpintero/a",               "Hogar y mantenimiento"),
        new Seed("CONSTRUCTION_WORKER",          "Obrero de Construcci\u00f3n","Hogar y mantenimiento"),
        new Seed("GARDENER",                     "Jardinero/a",                "Hogar y mantenimiento"),
        new Seed("AIR_CONDITIONING_TECHNICIAN",  "Instalador de A.A",          "Hogar y mantenimiento"),
        new Seed("CLEANER",                      "Personal de Limpieza",       "Hogar y mantenimiento"),
        new Seed("PILATES",                      "Instructora de pilates",     "Salud y bienestar"),
        new Seed("HAIRDRESSER",                  "Peluquero/a",                "Belleza"),
        new Seed("MECHANIC",                     "Mec\u00e1nico/a",            "Servicios"),
        new Seed("DRIVER",                       "Conductor",                  "Servicios"),
        new Seed("SECURITY",                     "Personal de seguridad",      "Servicios"),
        new Seed("RECEPTIONIST",                 "Recepcionista",              "Servicios"),
        new Seed("OTHER",                        "Otro",                       null)
    );

    @Override
    public void run(ApplicationArguments args) {
        int inserted = 0;
        for (Seed s : INITIAL_PROFESSIONS) {
            if (!professionRepo.existsByCode(s.code())) {
                Profession p = new Profession();
                p.setCode(s.code());
                p.setDisplayName(s.displayName());
                p.setCategory(s.category());
                p.setActive(true);
                professionRepo.save(p);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("ProfessionSeeder: {} profesiones insertadas", inserted);
        }
    }
}
