package nl.hackyourfuture.project.backend.auth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfo(
    String email,
    String name,
    @JsonProperty("email_verified") Object emailVerified
) {
  /**
   * Google's userinfo endpoint has been observed returning this field as
   * either a JSON boolean (true/false) or a quoted string ("true"/"false"),
   * depending on the call path. Parse defensively against both.
   */
  public boolean isEmailVerified() {
    if (emailVerified instanceof Boolean bool) {
      return bool;
    }
    if (emailVerified instanceof String str) {
      return Boolean.parseBoolean(str);
    }
    return false;
  }
}
