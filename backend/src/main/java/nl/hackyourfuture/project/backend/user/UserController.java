package nl.hackyourfuture.project.backend.user;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.user.dto.PatchUserRequest;
import nl.hackyourfuture.project.backend.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operations on user accounts")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Info about current user", description = "Returns all basic user information.")
    @ApiResponse(responseCode = "200", description = "User info")
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public UserResponse getCurrentUser(Authentication authentication) {
        UUID id = (UUID) authentication.getPrincipal();
        return userService.getCurrentUser(id);
    }


    @PatchMapping
    @Operation(summary = "Update an existing user", description = "Update the details of the current user.")
    @ApiResponse(responseCode = "200", description = "The updated user")
    @ApiResponse(
            responseCode = "400",
            description = "The request body is invalid",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public UserResponse updateUser(@Valid @RequestBody PatchUserRequest request, Authentication authentication) {
        UUID id = (UUID) authentication.getPrincipal();
        return userService.updateUser(id, request);
    }


    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the current user", description = "Permanently deletes the current user's account.")
    @ApiResponse(responseCode = "204", description = "User successfully deleted")
    @ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public void deleteUser(Authentication authentication) {
        UUID id = (UUID) authentication.getPrincipal();
        userService.deleteUser(id);
    }
}
