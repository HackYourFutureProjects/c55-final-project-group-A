package nl.hackyourfuture.project.backend.event.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EventQueryCriteria(
        String search,
        List<UUID> categoryIds,
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal radiusKm,
        EventPriceFilter price,
        List<EventTimeOfDay> timesOfDay
) {

    public EventQueryCriteria {
        search = search == null || search.isBlank()
                ? null
                : search.trim();

        categoryIds = categoryIds == null
                ? List.of()
                : categoryIds.stream()
                        .distinct()
                        .toList();

        timesOfDay = timesOfDay == null
                ? List.of()
                : timesOfDay.stream()
                        .distinct()
                        .toList();
    }

    public boolean hasCategoryFilter() {
        return !categoryIds.isEmpty();
    }

    public boolean hasAnyDateFilter() {
        return dateFrom != null || dateTo != null;
    }

    public boolean hasCompleteDateFilter() {
        return dateFrom != null && dateTo != null;
    }

    public boolean hasAnyLocationFilter() {
        return latitude != null
                || longitude != null
                || radiusKm != null;
    }

    public boolean hasCompleteLocationFilter() {
        return latitude != null
                && longitude != null
                && radiusKm != null;
    }

    public boolean hasPriceFilter() {
        return price != null;
    }

    public boolean hasTimeOfDayFilter() {
        return !timesOfDay.isEmpty();
    }
}
