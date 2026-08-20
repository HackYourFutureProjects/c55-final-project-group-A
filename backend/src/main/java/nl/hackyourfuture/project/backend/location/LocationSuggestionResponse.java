package nl.hackyourfuture.project.backend.location;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = """
        A single location suggestion returned by the geocoding service.
        Contains everything needed to display the suggestion in an
        autocomplete dropdown and, if the user selects it, to store
        it directly as an address record.
        """)
public record LocationSuggestionResponse(

    @Schema(
        description = "Identifier of this suggestion",
        example = "156006906",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String id,

    @Schema(
        description = "Full human-readable label, for display in the autocomplete dropdown",
        example = "Paradiso, Weteringschans, Amsterdam, North Holland, Netherlands",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String label,

    @Schema(
        description = "Street name, if available for this location",
        example = "Weteringschans",
        nullable = true
    )
    String street,

    @Schema(
        description = "House or building number, if available for this location",
        example = "6",
        nullable = true
    )
    String houseNumber,

    @Schema(
        description = "Postal code, if available for this location",
        example = "1017SG",
        nullable = true
    )
    String postalCode,

    @Schema(
        description = "Latitude of this location",
        example = "52.3612",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    BigDecimal latitude,

    @Schema(
        description = "Longitude of this location",
        example = "4.8828",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    BigDecimal longitude,

    @Schema(
        description = "City name, if available for this location",
        example = "Amsterdam",
        nullable = true
    )
    String cityName,

    @Schema(
        description = "Province, if available for this location",
        example = "North Holland",
        nullable = true
    )
    String province

) {
  public static LocationSuggestionResponse from(NominatimResult result) {
    return new LocationSuggestionResponse(
        String.valueOf(result.placeId()),
        result.displayName(),
        result.street(),
        result.houseNumber(),
        result.postalCode(),
        result.lat(),
        result.lon(),
        result.cityName(),
        result.province()
    );
  }
}
