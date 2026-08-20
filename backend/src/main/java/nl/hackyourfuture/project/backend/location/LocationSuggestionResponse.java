package nl.hackyourfuture.project.backend.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import nl.hackyourfuture.project.backend.location.LocationService;

@Schema(description = "A single location suggestion from the geocoding service")
public record LocationSuggestionResponse(
    @Schema(description = "Identifier used to resolve this suggestion into a full address later", example = "way/123456")
    String id,

    @Schema(description = "Human-readable label to show in the autocomplete dropdown", example = "Paradiso, Weteringschans, Amsterdam")
    String label
) {
  public static LocationSuggestionResponse from(LocationService.NominatimResult result){
    return new LocationSuggestionResponse(result.toLookupId(), result.displayName());
  }
}
