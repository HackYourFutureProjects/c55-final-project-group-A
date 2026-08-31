package nl.hackyourfuture.project.backend.weather;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenMeteoResult(
    @JsonProperty("hourly") Hourly hourly
) {
  public record Hourly(
      @JsonProperty("time") List<String> time,
      @JsonProperty("temperature_2m") List<Double> temperature,
      @JsonProperty("precipitation_probability") List<Integer> precipitationProbability,
      @JsonProperty("windspeed_10m") List<Double> windSpeed,
      @JsonProperty("weathercode") List<Integer> weatherCode
  ){}
}
