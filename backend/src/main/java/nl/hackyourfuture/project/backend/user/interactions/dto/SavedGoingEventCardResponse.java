package nl.hackyourfuture.project.backend.user.interactions.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.user.interactions.SavedGoingEventCard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "An event as shown on a saved/going event card in the user's personal account")
public record SavedGoingEventCardResponse (
    @Schema(
        description = "Unique identifier of the event",
        example = "40000000-0000-0000-0000-000000000001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID id,

    @Schema(
        description = "Title of the event",
        example = "Amsterdam Music Night",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String title,

    @Schema(
        description = "URL of the event's primary image",
        example = "https://example.com/images/music-night.jpg",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String imageUrl,

    @Schema(
        description = "Categories assigned to the event",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<CategoryResponse> categories,

    @Schema(
        description = "Date and time when the event starts",
        example = "2026-09-12T17:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    OffsetDateTime startAt,

    @Schema(
        description = "Date and time when the event ends",
        example = "2026-09-12T21:30:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    OffsetDateTime endAt,

    @Schema(
        description = "Event price in euros",
        example = "24.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    BigDecimal price,

    @Schema(
        description = "Location where the event takes place",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    AddressSummaryResponse address,

    @Schema(
        description = "Whether the event has been cancelled by an admin",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isCancelled
){
  public static SavedGoingEventCardResponse from (SavedGoingEventCard e){
    return new SavedGoingEventCardResponse(
        e.id(),
        e.title(),
        e.imageUrl(),
        e.categories().stream().map(c -> new CategoryResponse(c.id(), c.name())).toList(),
        e.startAt(),
        e.endAt(),
        e.price(),
        new AddressSummaryResponse(e.street(), e.houseNumber(), e.cityName(), e.province()),
        e.isCancelled()
    );

  }

  @Schema(description = "A category assigned to the event")
  public record CategoryResponse(
      @Schema(
          description = "Unique identifier of the category",
          example = "767ba856-84a0-4818-91d3-ec9100c23a92",
          requiredMode = Schema.RequiredMode.REQUIRED
      )
      UUID id,

      @Schema(
          description = "Name of the category",
          example = "Music",
          requiredMode = Schema.RequiredMode.REQUIRED
      )
      String name){}

  @Schema(description = "Short address summary shown on an event card")
  public record AddressSummaryResponse(
      @Schema(
          description = "Street name",
          example = "Weteringschans",
          requiredMode = Schema.RequiredMode.REQUIRED
      )
      String street,
      @Schema(
          description = "House or building number",
          example = "6",
          nullable = true
      )
      String houseNumber,
      @Schema(
          description = "City name",
          example = "Amsterdam",
          requiredMode = Schema.RequiredMode.REQUIRED
      )
      String cityName,
      @Schema(
          description = "Province",
          example = "North Holland",
          nullable = true
      )
      String province
  ){}
}

