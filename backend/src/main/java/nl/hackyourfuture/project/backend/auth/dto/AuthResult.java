package nl.hackyourfuture.project.backend.auth.dto;

public record AuthResult(AuthResponse response, String rawAccessToken) {
}
