package nl.hackyourfuture.project.backend.weather;

import lombok.RequiredArgsConstructor;
import nl.hackyourfuture.project.backend.location.ExternalServiceException;
import nl.hackyourfuture.project.backend.user.exceptions.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class WeatherService {

  private static final String OPEN_METEO_BASE_URL = "https://api.open-meteo.com";

  private final RestClient restClient =  RestClient.builder()
      .baseUrl(OPEN_METEO_BASE_URL)
      .defaultHeader("User-Agent", "HackYourFuture-EventApp/1.0")
      .build();

  public WeatherResponse getWeather(BigDecimal latitude, BigDecimal longitude, OffsetDateTime eventTime){
    if (latitude == null) {
      throw new BadRequestException("Latitude must be provided");
    }
    if (longitude == null) {
      throw new BadRequestException("Longitude must be provided");
    }
    if (eventTime == null) {
      throw new BadRequestException("Event time must be provided");
    }

    ZoneId zone = ZoneId.of("Europe/Amsterdam");
    OffsetDateTime localTime = eventTime.atZoneSameInstant(zone).toOffsetDateTime();

    String targetHour = localTime
        .truncatedTo(ChronoUnit.HOURS)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

    OpenMeteoResult result;

    try{
      result = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/v1/forecast")
              .queryParam("latitude", latitude)
              .queryParam("longitude", longitude)
              .queryParam("hourly", "temperature_2m,precipitation_probability,windspeed_10m,weathercode")
              .queryParam("forecast_days", 16)
              .queryParam("timezone", "auto")
              .build())
          .retrieve()
          .body(OpenMeteoResult.class);

    } catch (RestClientException e){
      throw new ExternalServiceException("Failed to fetch weather data from Open-Meteo");
    }

    int index = result.hourly().time().indexOf(targetHour);

    if (index == -1) {
      return WeatherResponse.unavailable();
    }

    var h = result.hourly();

    if (h.temperature().get(index) == null ||
        h.precipitationProbability().get(index) == null ||
        h.windSpeed().get(index) == null ||
        h.weatherCode().get(index) == null) {

      return WeatherResponse.unavailable();
    }

    return WeatherResponse.from(result.hourly(), index);
  }
}
