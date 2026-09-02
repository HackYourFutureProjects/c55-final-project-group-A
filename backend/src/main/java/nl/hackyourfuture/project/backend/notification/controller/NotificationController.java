package nl.hackyourfuture.project.backend.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationPageResponse;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationResponse;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationUnreadCountResponse;
import nl.hackyourfuture.project.backend.notification.service.NotificationService;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/unread-count")
    @Operation(
            summary = "Get unread notification count",
            description = "Returns how many unread notifications the authenticated user has."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Unread count returned successfully",
            content = @Content(
                    schema = @Schema(implementation = NotificationUnreadCountResponse.class)
            )
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
    public NotificationUnreadCountResponse getUnreadCount(
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return notificationService.getUnreadCount(userId);
    }

    @PostMapping("/{id}/open")
    @Operation(
            summary = "Open a notification",
            description = """
                    Marks the notification as read and returns it.
                    If the notification is already read, returns it unchanged.
                    Returns 404 when the notification does not exist or does not
                    belong to the authenticated user.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Notification opened successfully",
            content = @Content(
                    schema = @Schema(implementation = NotificationResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Notification or user not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    public NotificationResponse openNotification(
            @Parameter(
                    description = "ID of the notification to open",
                    example = "50000000-0000-0000-0000-000000000001"
            )
            @PathVariable UUID id,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return notificationService.openNotification(userId, id);
    }

    @PostMapping("/read-all")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks every unread notification for the authenticated user as read."
    )
    @ApiResponse(
            responseCode = "200",
            description = "All unread notifications marked as read"
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
    public void markAllAsRead(
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        notificationService.markAllAsRead(userId);
    }
}
