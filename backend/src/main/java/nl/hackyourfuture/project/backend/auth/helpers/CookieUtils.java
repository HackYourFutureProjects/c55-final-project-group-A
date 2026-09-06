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
  private static final String STATE_COOKIE_NAME = "google_oauth_state";
  private static final Duration STATE_MAX_AGE = Duration.ofMinutes(5);


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

  //GoogleOAuth
  public void setStateCookie(HttpServletResponse response, String state) {
    ResponseCookie stateCookie = ResponseCookie.from(STATE_COOKIE_NAME, state)
        .httpOnly(true)
        .secure(true)
        .path("/api/auth/google")
        .maxAge(STATE_MAX_AGE)
        .sameSite("Lax")
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());
  }

  public void clearStateCookie(HttpServletResponse response) {
    ResponseCookie cleared = ResponseCookie.from(STATE_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .path("/api/auth/google")
        .maxAge(0)
        .sameSite("Lax")
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
  }

  public String extractStateCookie(HttpServletRequest request) {
    return Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
        .filter(c -> STATE_COOKIE_NAME.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

}
