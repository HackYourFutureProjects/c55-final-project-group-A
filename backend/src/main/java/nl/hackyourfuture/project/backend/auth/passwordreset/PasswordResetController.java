package nl.hackyourfuture.project.backend.auth.passwordreset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.auth.dto.AuthResponse;
import nl.hackyourfuture.project.backend.auth.dto.AuthResult;
import nl.hackyourfuture.project.backend.auth.helpers.CookieUtils;
import nl.hackyourfuture.project.backend.auth.passwordreset.dto.ForgotPasswordRequest;
import nl.hackyourfuture.project.backend.auth.passwordreset.dto.ResetPasswordRequest;
import nl.hackyourfuture.project.backend.auth.passwordreset.dto.TokenValidationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class PasswordResetController {

  private final PasswordResetService passwordResetService;
  private final CookieUtils cookieUtils;

  @PostMapping("/forgot-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Request a password reset",
      description = """
                    Sends a password reset link to the given email if an account \
                    exists for it. Always returns 204, regardless of whether the \
                    email is registered — this prevents attackers from using this \
                    endpoint to discover which emails have accounts.
                    """
  )
  @ApiResponse(responseCode = "204", description = "Request processed")
  @ApiResponse(
      responseCode = "400",
      description = "The request body is invalid",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestPasswordReset(request.email());
  }

  @GetMapping("/reset-password/validate")
  @Operation(
      summary = "Validate a reset token",
      description = "Checks whether a password reset token is still valid, before showing the reset form."
  )
  @ApiResponse(responseCode = "200", description = "Validation result")
  public TokenValidationResponse validateResetToken(
      @Parameter(description = "The reset token from the emailed link")
      @RequestParam String token
  ) {
    return new TokenValidationResponse(passwordResetService.isTokenValid(token));
  }

  @PostMapping("/reset-password")
  @Operation(
      summary = "Complete a password reset",
      description = """
                    Sets a new password using a valid reset token. On success, \
                    all existing sessions for the account are invalidated and a \
                    new session is created — the user is signed in automatically.
                    """
  )
  @ApiResponse(responseCode = "200", description = "Password reset successfully, user is signed in")
  @ApiResponse(
      responseCode = "400",
      description = "The reset token is invalid, expired, or already used",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public AuthResponse resetPassword(
      @Valid @RequestBody ResetPasswordRequest request,
      HttpServletResponse response
  ) {
    AuthResult result = passwordResetService.resetPassword(request.token(), request.newPassword());
    cookieUtils.setSessionCookie(response, result.rawAccessToken());
    return result.response();
  }
}
