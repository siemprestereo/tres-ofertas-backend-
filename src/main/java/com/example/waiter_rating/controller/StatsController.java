package com.example.waiter_rating.controller;

import com.example.waiter_rating.repository.RatingRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final RatingRepo ratingRepository;

    public StatsController(RatingRepo ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @GetMapping("/professional/{professionalId}/by-month")
    public ResponseEntity<?> getRatingsByMonth(@PathVariable Long professionalId) {
        var ratings = ratingRepository.findByProfessionalId(professionalId);

        // Agrupar por mes
        Map<String, List<Integer>> byMonth = ratings.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toString().substring(0, 7), // YYYY-MM
                        Collectors.mapping(r -> r.getScore(), Collectors.toList())
                ));

        // Calcular promedio por mes
        List<Map<String, Object>> result = byMonth.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("month", entry.getKey());
                    monthData.put("average", entry.getValue().stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0));
                    monthData.put("count", entry.getValue().size());
                    return monthData;
                })
                .sorted((a, b) -> ((String) a.get("month")).compareTo((String) b.get("month")))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/professional/{professionalId}/by-business")
    public ResponseEntity<?> getRatingsByBusiness(@PathVariable Long professionalId) {
        var ratings = ratingRepository.findByProfessionalId(professionalId);

        // Agrupar por business
        Map<String, List<Integer>> byBusiness = ratings.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBusiness().getName(),
                        Collectors.mapping(r -> r.getScore(), Collectors.toList())
                ));

        // Calcular promedio por business
        List<Map<String, Object>> result = byBusiness.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> businessData = new HashMap<>();
                    businessData.put("business", entry.getKey());
                    businessData.put("average", entry.getValue().stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0));
                    businessData.put("count", entry.getValue().size());
                    return businessData;
                })
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("average"),
                        (Double) a.get("average")
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/professional/{professionalId}/by-profession-type")
    public ResponseEntity<?> getRatingsByProfessionType(@PathVariable Long professionalId) {
        var ratings = ratingRepository.findByProfessionalId(professionalId);

        if (ratings.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Obtener el tipo de profesión
        String professionType = ratings.get(0).getProfessional().getProfessionType().toString();

        // Calcular estadísticas generales
        Map<String, Object> stats = new HashMap<>();
        stats.put("professionType", professionType);
        stats.put("totalRatings", ratings.size());
        stats.put("averageScore", ratings.stream()
                .mapToInt(r -> r.getScore())
                .average()
                .orElse(0.0));

        return ResponseEntity.ok(stats);
    }
}