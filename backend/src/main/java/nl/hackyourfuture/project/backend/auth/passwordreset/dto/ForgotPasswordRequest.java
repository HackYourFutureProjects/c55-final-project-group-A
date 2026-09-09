package nl.hackyourfuture.project.backend.auth.passwordreset.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to start a password reset")
public record ForgotPasswordRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "Email address of the account to reset", example = "anouk.devries@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email
) {}