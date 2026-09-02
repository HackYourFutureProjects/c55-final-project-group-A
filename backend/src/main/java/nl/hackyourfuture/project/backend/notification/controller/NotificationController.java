package nl.hackyourfuture.project.backend.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationPageResponse;
import nl.hackyourfuture.project.backend.notification.service.NotificationService;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(
        name = "Notifications",
        description = "Operations for reading and managing the current user's in-app notifications"
)
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @SecurityRequirements
    @Operation(
            summary = "List notifications for the current user",
            description = """
                    Returns one page of in-app notifications for the authenticated user,
                    ordered from newest to oldest. By default both read and unread
                    notifications are included. Set unreadOnly=true to return only
                    unread notifications.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Notifications returned successfully",
            content = @Content(
                    schema = @Schema(implementation = NotificationPageResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Page or size is outside the allowed range",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public NotificationPageResponse getNotifications(
            @Parameter(
                    description = "Zero-based page number",
                    example = "0"
            )
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(
                    description = "Number of notifications per page, between 1 and 100",
                    example = "20"
            )
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100")
            int size,

            @Parameter(
                    description = """
                            When true, returns only unread notifications.
                            When false or omitted, returns both read and unread notifications.
                            """,
                    example = "false"
            )
            @RequestParam(defaultValue = "false")
            boolean unreadOnly,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return notificationService.getNotificationPage(
                userId,
                page,
                size,
                unreadOnly
        );
    }
}