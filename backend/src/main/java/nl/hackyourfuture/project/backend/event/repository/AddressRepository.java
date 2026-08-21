package nl.hackyourfuture.project.backend.event.repository;


import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.event.dto.request.EventAddressRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AddressRepository {

    private final JdbcClient jdbcClient;

    public UUID create(EventAddressRequest address) {
        String sql = """
                INSERT INTO addresses (
                    street,
                    house_number,
                    postal_code,
                    latitude,
                    longitude,
                    city_name,
                    province
                )
                VALUES (
                    :street,
                    :houseNumber,
                    :postalCode,
                    :latitude,
                    :longitude,
                    :cityName,
                    :province
                )
                RETURNING id
                """;

        return jdbcClient
                .sql(sql)
                .param("street", address.street())
                .param("houseNumber", address.houseNumber())
                .param("postalCode", address.postalCode())
                .param("latitude", address.latitude())
                .param("longitude", address.longitude())
                .param("cityName", address.cityName())
                .param("province", address.province())
                .query(UUID.class)
                .single();
    }
}