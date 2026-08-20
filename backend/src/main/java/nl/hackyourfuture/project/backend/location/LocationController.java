package nl.hackyourfuture.project.backend.location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Location", description = "Address autocomplete via external geocoding service")
public class LocationController {

  private final LocationService locationService;

  @GetMapping("/suggest")
  @Operation(summary = "Get address suggestions",
      description = """
                      Returns a list of matching locations for the given search text,
                      backed by an external geocoding service (Nominatim).
          
                      Each suggestion includes coordinates and, where available, a
                      structured address breakdown (street, house number, postal code,
                      city, province). Street-level fields may be null for suggestions
                      that represent a whole city or area rather than a specific
                      address — house number in particular may be missing even for
                      some specific venues.
          
          """)
  @ApiResponse(responseCode = "200", description = "List of matching suggestions")
  @ApiResponse(
      responseCode = "400",
      description = "Search query is missing or blank",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "503",
      description = "The external geocoding service is temporarily unavailable",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public List<LocationSuggestionResponse> suggest(
      @Parameter(description = "Text to search for. Works best with complete words (e.g. \\\"Amsterdam\\\" rather than \\\"Amst\\\") — the underlying service doesn't reliably match partial words mid-typing.", example = "Paradiso Amsterdam")
      @RequestParam String q) {
    return locationService.suggest(q);
  }
}
