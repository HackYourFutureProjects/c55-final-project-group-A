package nl.hackyourfuture.project.backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "The details needed to update a user")
public record PatchUserRequest(
    @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters long")
    @Schema(description = "Name of the user", example = "Anouk de Vries")
    String name,

    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Email(message = "Please provide a valid email address")
    @Schema(description = "Email address of the user", example = "anouk.devries@example.com")
    String email,

    @Size(max = 100, message = "Location must not exceed 100 characters")
    @Schema(description = "User location", example = "Amsterdam")
    String location
) {
}
