package nl.hackyourfuture.project.backend.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Address where the event takes place")
public record EventAddressRequest(

        @NotBlank(message = "Please provide a street")
        @Schema(
                description = "Street where the event takes place",
                example = "Weteringschans",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String street,

        @Schema(
                description = "House or building number",
                example = "6",
                nullable = true
        )
        String houseNumber,

        @Schema(
                description = "Postal code",
                example = "1017SG",
                nullable = true
        )
        String postalCode,

        @NotNull(message = "Please provide latitude")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        @Schema(
                description = "Latitude, between -90 and 90",
                example = "52.3612",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal latitude,

        @NotNull(message = "Please provide longitude")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        @Schema(
                description = "Longitude, between -180 and 180",
                example = "4.8828",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal longitude,

        @NotBlank(message = "Please provide a city")
        @Schema(
                description = "City where the event takes place",
                example = "Amsterdam",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cityName,

        @Schema(
                description = "Province or region",
                example = "North Holland",
                nullable = true
        )
        String province
) {
}