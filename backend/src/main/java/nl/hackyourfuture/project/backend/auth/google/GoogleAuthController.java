package nl.hackyourfuture.project.backend.auth.google;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.dto.AuthResult;
import nl.hackyourfuture.project.backend.auth.helpers.CookieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class GoogleAuthController {

  private final GoogleAuthService googleAuthService;
  private final CookieUtils cookieUtils;
  @Value("${frontend.base-url}")
  private String frontendBaseUrl;

  @GetMapping
  @Operation(
      summary = "Start Google sign-in",
      description = """
          Redirects the browser to Google's sign-in page. Not meant to be \
          called via fetch/XHR — use a plain link or full-page navigation \
          (e.g. `<a href="/api/auth/google">Continue with Google</a>`). \
          After the user signs in, they'll be redirected back to the \
          frontend with a session cookie already set.
          """
  )
  @ApiResponse(responseCode = "302", description = "Redirects to Google's sign-in page")
  public void redirectToGoogle(HttpServletResponse response) throws IOException {
    String state = googleAuthService.generateState();
    cookieUtils.setStateCookie(response, state);
    response.sendRedirect(googleAuthService.buildAuthorizationUrl(state));
  }

  @GetMapping("/callback")
  @Operation(
      summary = "Google sign-in callback",
      description = """
          Called by Google after the user signs in — not meant to be \
          called directly by the frontend. On success, redirects to the \
          frontend's base URL with a session cookie set. On failure, \
          redirects to `/login?error=invalid_state` or \
          `/login?error=google_auth_failed`, or `/login?error=unexpected_error`.
          """
  )
  @ApiResponse(responseCode = "302", description = "Success — redirects to the frontend's base URL with a session cookie set")
  @ApiResponse(responseCode = "400", description = "Missing 'code' or 'state' query parameter")
  public void handleCallback(
      @RequestParam("code") String code,
      @RequestParam("state") String state,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    String expectedState = cookieUtils.extractStateCookie(request);

    cookieUtils.clearStateCookie(response);

    if (expectedState == null || !expectedState.equals(state)) {
      response.sendRedirect(frontendBaseUrl + "/login?error=invalid_state");
      return;
    }

    try {
      AuthResult result = googleAuthService.handleCallback(code);
      cookieUtils.setSessionCookie(response, result.rawAccessToken());
      response.sendRedirect(frontendBaseUrl);

    } catch (GoogleAuthException e) {
      response.sendRedirect(frontendBaseUrl + "/login?error=google_auth_failed");
    } catch (Exception e) {
      response.sendRedirect(frontendBaseUrl + "/login?error=unexpected_error");
    }
  }

}
