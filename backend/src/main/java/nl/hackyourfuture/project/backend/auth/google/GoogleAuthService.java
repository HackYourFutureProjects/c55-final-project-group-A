package nl.hackyourfuture.project.backend.auth.google;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.AuthService;
import nl.hackyourfuture.project.backend.auth.dto.AuthResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

  private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
  private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private static final String GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
  private final RestClient restClient = RestClient.create();
  private final AuthService authService;
  @Value("${google.client-id}")
  private String clientId;
  @Value("${google.client-secret}")
  private String clientSecret;
  @Value("${google.redirect-uri}")
  private String redirectUri;

  public String generateState() {
    return UUID.randomUUID().toString();
  }

  public String buildAuthorizationUrl(String state) {
    return UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", redirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", "openid email profile")
        .queryParam("state", state)
        .build()
        .toUriString();
  }

  public AuthResult handleCallback(String code) {
    String accessToken = exchangeCodeForAccessToken(code);
    GoogleUserInfo userInfo = fetchUserInfo(accessToken);

    if (!userInfo.isEmailVerified()) {
      throw new GoogleAuthException("Google account email is not verified");
    }

    return authService.loginOrRegisterFromGoogle(userInfo.email(), userInfo.name());
  }

  private String exchangeCodeForAccessToken(String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("redirect_uri", redirectUri);
    form.add("grant_type", "authorization_code");

    try {
      GoogleTokenResponse response = restClient.post()
          .uri(GOOGLE_TOKEN_URL)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(GoogleTokenResponse.class);

      return response.accessToken();
    } catch (RestClientException e) {
      throw new GoogleAuthException("Failed to exchange authorization code with Google");
    }
  }

  private GoogleUserInfo fetchUserInfo(String accessToken) {
    try {
      return restClient.get()
          .uri(GOOGLE_USERINFO_URL)
          .header("Authorization", "Bearer " + accessToken)
          .retrieve()
          .body(GoogleUserInfo.class);

    } catch (RestClientException e) {
      throw new GoogleAuthException("Failed to fetch user info from Google");
    }
  }

}
