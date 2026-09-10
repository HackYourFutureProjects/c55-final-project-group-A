package nl.hackyourfuture.project.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change the current user's password")
public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    @Schema(description = "The user's current password", requiredMode = Schema.RequiredMode.REQUIRED)
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters long")
    @Schema(description = "The new password", example = "NewSecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
    String newPassword
) {
}
