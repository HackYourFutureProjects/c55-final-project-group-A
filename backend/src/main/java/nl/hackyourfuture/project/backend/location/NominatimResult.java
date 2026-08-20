package nl.hackyourfuture.project.backend.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;


@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResult(
    @JsonProperty("place_id") long placeId,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("lat") BigDecimal lat,
    @JsonProperty("lon") BigDecimal lon,
    @JsonProperty("address") Map<String, String> address
) {
  public String street() {
    return address != null ? address.get("road") : null;
  }

  public String houseNumber() {
    return address != null ? address.get("house_number") : null;
  }

  public String postalCode() {
    return address != null ? address.get("postcode") : null;
  }

  public String cityName() {
    if (address == null) return null;
    if (address.containsKey("city")) return address.get("city");
    if (address.containsKey("town")) return address.get("town");
    return address.get("village");
  }

  public String province() {
    return address != null ? address.get("state") : null;
  }
}