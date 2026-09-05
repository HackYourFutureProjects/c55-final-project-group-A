package nl.hackyourfuture.project.backend.auth.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfo (
    String email,
    String name
){
}
