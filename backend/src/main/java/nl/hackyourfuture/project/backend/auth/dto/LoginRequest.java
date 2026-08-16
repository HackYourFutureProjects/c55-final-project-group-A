package nl.hackyourfuture.project.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


@Schema(description = "The details needed to log in a user")
public record LoginRequest(
    @NotBlank(message = "Please provide an email")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "Email address of the user", example = "anouk.devries@example.com")
    String email,

    @NotBlank(message = "Please provide a password")
    @Schema(description = "Password of the user", example = "password2026")
    String password) {
}

