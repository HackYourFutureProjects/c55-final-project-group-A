package nl.hackyourfuture.project.backend.auth.helpers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class CookieUtils {

  private static final String SESSION_COOKIE_NAME = "session_access_token";
  private static final Duration SESSION_MAX_AGE = Duration.ofHours(2);

  public void setSessionCookie(HttpServletResponse response, String rawToken) {
    ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, rawToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(SESSION_MAX_AGE)
        .sameSite("Lax")
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void clearSessionCookie(HttpServletResponse response) {
    ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("Lax")
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public String extractToken(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> c.getName().equals(SESSION_COOKIE_NAME))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }
}
