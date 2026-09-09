package nl.hackyourfuture.project.backend.auth.passwordreset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to complete a password reset")
public record ResetPasswordRequest(

    @NotBlank(message = "Token is required")
    @Schema(description = "The reset token from the emailed link", example = "a1b2c3d4-...", requiredMode = Schema.RequiredMode.REQUIRED)
    String token,

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters long")
    @Schema(description = "The new password", example = "newSecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
    String newPassword

) {}