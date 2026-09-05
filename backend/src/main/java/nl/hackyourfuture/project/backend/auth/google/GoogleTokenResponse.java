package nl.hackyourfuture.project.backend.auth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleTokenResponse(
    @JsonProperty("access_token") String accessToken
) {
}
