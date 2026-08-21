package nl.hackyourfuture.project.backend.event.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Schema(
        description = """
                Fields an administrator can update on an existing event.
                Omitted fields remain unchanged.
                """
)
public record UpdateEventRequest(

        @Pattern(
                regexp = ".*\\S.*",
                message = "Title must not be blank"
        )
        @Size(
                max = 255,
                message = "Title must not exceed 255 characters"
        )
        @Schema(
                description = "New event title; omit to keep the current title",
                example = "Updated Synth Night",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String title,

        @Schema(
                description = """
                        New event description. Omit or send null to keep the
                        current description. Send an empty or blank string to
                        remove the current description.
                        """,
                example = "Updated event description",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String description,

        @Size(
                min = 1,
                message = "Please provide at least one category"
        )
        @Schema(
                description = """
                        Complete replacement set of category IDs.
                        Omit to keep the current categories.
                        """,
                example = "[\"40000000-0000-0000-0000-000000000001\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Set<
                @NotNull(message = "Category ID cannot be null")
                        UUID
                > categoryIds,

        @Valid
        @Schema(
                description = """
                        Complete replacement address selected from the location
                        service. Omit to keep the current address.
                        """,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        EventAddressRequest address,

        @Schema(
                description = """
                        New event start date and time.
                        Omit to keep the current start time.
                        """,
                example = "2026-09-13T21:00:00+02:00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        OffsetDateTime startAt,

        @Schema(
                description = """
                        New event end date and time.
                        Omit to keep the current end time.
                        """,
                example = "2026-09-14T03:00:00+02:00",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        OffsetDateTime endAt,

        @DecimalMin(
                value = "0.00",
                message = "Price cannot be negative"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = """
                        Price must have at most eight integer digits
                        and two decimal places
                        """
        )
        @Schema(
                description = """
                        New event price in euros. Use zero for a free event.
                        Omit to keep the current price.
                        """,
                example = "25.00",
                minimum = "0",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        BigDecimal price
) {
}