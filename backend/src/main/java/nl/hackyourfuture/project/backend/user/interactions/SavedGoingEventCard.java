package nl.hackyourfuture.project.backend.user.interactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SavedGoingEventCard(
    UUID id,
    String title,
    String imageUrl,
    List<CategoryName> categories,
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    BigDecimal price,
    String street,
    String houseNumber,
    String cityName,
    String province,
    boolean isCancelled
) {
  public record CategoryName(UUID id, String name) {
  }
}
