package nl.hackyourfuture.project.backend.auth;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import nl.hackyourfuture.project.backend.auth.dto.AuthResponse;
import nl.hackyourfuture.project.backend.auth.dto.AuthResult;
import nl.hackyourfuture.project.backend.auth.dto.LoginRequest;
import nl.hackyourfuture.project.backend.auth.dto.RegisterRequest;
import nl.hackyourfuture.project.backend.auth.helpers.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
@AllArgsConstructor
@Tag(name = "Auth", description = "Operations for user registration, login, and logout")
public class AuthController {

  private final AuthService authService;
  private final CookieUtils cookieUtils;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @SecurityRequirements
  @Operation(summary = "Register a new user", description = "Creates a new user account, starts a session, and returns the user's details.")
  @ApiResponse(responseCode = "201", description = "The user was created and is now logged in")
  @ApiResponse(
      responseCode = "400",
      description = "The request body is invalid",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "An account with this email already exists",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
    AuthResult result = authService.register(request);
    cookieUtils.setSessionCookie(response, result.rawAccessToken());

    return result.response();
  }

  @PostMapping("/login")
  @SecurityRequirements
  @Operation(summary = "Log in an existing user", description = "Verifies the user's credentials, starts a session, and returns the user's details.")
  @ApiResponse(responseCode = "200", description = "Login successful")
  @ApiResponse(
      responseCode = "400",
      description = "The request body is invalid",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "401",
      description = "Email or password is incorrect",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthResult result = authService.login(request);
    cookieUtils.setSessionCookie(response, result.rawAccessToken());
    return result.response();
  }

  @DeleteMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Log out the current user", description = "Invalidates the current session and clears the session cookie.")
  @ApiResponse(responseCode = "204", description = "Logout successful")
  @ApiResponse(
      responseCode = "401",
      description = "No valid session found",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String rawAccessToken = cookieUtils.extractToken(request);
    if (rawAccessToken != null) {
      authService.logout(rawAccessToken);
    }
    cookieUtils.clearSessionCookie(response);
  }

}
