package nl.hackyourfuture.project.backend.auth.passwordreset.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Whether a password reset token is still valid")
public record TokenValidationResponse(

    @Schema(description = "True if the token exists, has not expired, and has not been used",
        example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean valid

) {}