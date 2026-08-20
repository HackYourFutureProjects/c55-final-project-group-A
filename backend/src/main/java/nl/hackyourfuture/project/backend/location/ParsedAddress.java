package nl.hackyourfuture.project.backend.location;

import java.math.BigDecimal;

public record ParsedAddress(
    String street,
    String houseNumber,
    String postalCode,
    String cityName,
    String province,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
