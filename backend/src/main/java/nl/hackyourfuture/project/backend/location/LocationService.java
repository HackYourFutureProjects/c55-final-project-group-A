package nl.hackyourfuture.project.backend.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.location.dto.LocationSuggestionResponse;
import nl.hackyourfuture.project.backend.location.exceptions.ExternalServiceException;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LocationService {

  private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";

  private final RestClient restClient = RestClient.builder()
      .baseUrl(NOMINATIM_BASE_URL)
      .defaultHeader("User-Agent", "HackYourFuture-EventApp/1.0")
      .build();

  public List<LocationSuggestionResponse> suggest(String query) {
    if (query == null || query.isBlank()) {
      throw new BadRequestException("Please write your query");
    }

    try {
      NominatimResult[] results = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/search")
              .queryParam("q", query)
              .queryParam("format", "json")
              .queryParam("addressdetails", 1)
              .queryParam("countrycodes", "nl")
              .queryParam("limit", 5)
              .build())
          .retrieve()
          .body(NominatimResult[].class);

      if (results == null) {
        return List.of();
      }

      return Stream.of(results)
          .map(LocationSuggestionResponse::from)
          .toList();
    } catch (RestClientException e) {
      throw new ExternalServiceException("Location search is temporarily unavailable");
    }

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record NominatimResult(
      @JsonProperty("place_id")
      String placeId,

      @JsonProperty("display_name")
      String displayName
  ) {
  }
}
