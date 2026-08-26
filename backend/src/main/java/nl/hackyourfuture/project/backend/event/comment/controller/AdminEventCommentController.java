package nl.hackyourfuture.project.backend.event.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.comment.dto.request.AdminReplyRequest;
import nl.hackyourfuture.project.backend.event.comment.dto.response.EventCommentResponse;
import nl.hackyourfuture.project.backend.event.comment.service.AdminEventCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@Tag(
        name = "Admin event comments",
        description = "Admin-only operations for managing event comments and replies"
)
public class AdminEventCommentController {

    private final AdminEventCommentService adminEventCommentService;

    @PostMapping("/{commentId}/reply")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Reply to a comment",
            description = """
                    Adds an official admin reply to an existing event comment.
                    A comment can have only one admin reply.
                    """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Admin reply created successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = EventCommentResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The comment ID or reply content is invalid",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only an administrator can reply to comments",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The comment does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The comment already has an admin reply",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public EventCommentResponse createAdminReply(
            @Parameter(
                    description = "Unique identifier of the comment",
                    example = "50000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID commentId,

            @Valid
            @RequestBody
            AdminReplyRequest request,

            @Parameter(hidden = true)
            Authentication authentication
    ) {
        UUID adminUserId = (UUID) authentication.getPrincipal();

        return adminEventCommentService.createAdminReply(
                commentId,
                adminUserId,
                request
        );
    }

    @PatchMapping("/{commentId}/reply")
    @Operation(
            summary = "Update an admin reply",
            description = """
                    Updates the existing official admin reply attached to a
                    comment. The comment must already have an admin reply.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Admin reply updated successfully",
            content = @Content(
                    schema = @Schema(
                            implementation = EventCommentResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "The comment ID or reply content is invalid",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only an administrator can update replies",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The comment or its admin reply does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public EventCommentResponse updateAdminReply(
            @Parameter(
                    description = "Unique identifier of the comment",
                    example = "50000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID commentId,

            @Valid
            @RequestBody
            AdminReplyRequest request
    ) {
        return adminEventCommentService.updateAdminReply(
                commentId,
                request
        );
    }

    @DeleteMapping("/{commentId}/reply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete an admin reply",
            description = """
                    Permanently removes the official admin reply from a comment.
                    The original user comment remains available.
                    """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Admin reply deleted successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "The supplied comment ID is not a valid UUID",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only an administrator can delete replies",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The comment or its admin reply does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void deleteAdminReply(
            @Parameter(
                    description = "Unique identifier of the comment",
                    example = "50000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID commentId
    ) {
        adminEventCommentService.deleteAdminReply(commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete any event comment",
            description = """
                    Permanently deletes an event comment and its admin reply,
                    when one exists. Administrators can delete comments created
                    by any user.
                    """
    )
    @ApiResponse(
            responseCode = "204",
            description = "Comment deleted successfully"
    )
    @ApiResponse(
            responseCode = "400",
            description = "The supplied comment ID is not a valid UUID",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is required",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Only an administrator can delete any user’s comment",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The comment does not exist",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void deleteComment(
            @Parameter(
                    description = "Unique identifier of the comment",
                    example = "50000000-0000-0000-0000-000000000001",
                    required = true
            )
            @PathVariable
            UUID commentId
    ) {
        adminEventCommentService.deleteComment(commentId);
    }
}