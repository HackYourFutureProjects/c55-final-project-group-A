package nl.hackyourfuture.project.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Schema(description = "The details needed to register a new user")
public record RegisterRequest(
    @NotBlank(message = "Please provide a name")
    @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters long")
    @Schema(description = "Name of the user", example = "Anouk de Vries")
    String name,

    @NotBlank(message = "Please provide an email")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "Email address of the user", example = "anouk.devries@example.com")
    String email,

    @NotBlank(message = "Please provide an password")
    @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters long")
    @Schema(description = "Password of the user", example = "password2026")
    String password) {
}


