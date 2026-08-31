package nl.hackyourfuture.project.backend.weather;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
    Normalized weather information for a specific location and hour.
    Returned by the backend after cleaning and transforming the raw
    Open-Meteo API response into a frontend-friendly format.
    """)
public record WeatherResponse(
    @Schema(
        description = "Indicates whether valid weather data is available for the requested location",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean isAvailable,
    @Schema(
        description = "Current air temperature in degrees Celsius",
        example = "17",
        nullable = true
    )
    int temperature,
    @Schema(
        description = "Short textual description of the current weather conditions",
        example = "Partly cloudy",
        nullable = true
    )
    String condition,
    @Schema(
        description = "Probability of precipitation expressed as a percentage",
        example = "12",
        nullable = true
    )
    int precipitationChance,
    @Schema(
        description = "Wind speed in meters per second",
        example = "9",
        nullable = true
    )
    int windSpeed
) {
  public static WeatherResponse from(OpenMeteoResult.Hourly hourly, int index) {
    return new WeatherResponse(
        true,
        hourly.temperature().get(index).intValue(),
        weatherCodeToDescription(hourly.weatherCode().get(index)),
        hourly.precipitationProbability().get(index),
        hourly.windSpeed().get(index).intValue()
    );
  }

  public static String weatherCodeToDescription(int code) {
    return switch (code) {
      case 0 -> "Clear sky";
      case 1 -> "Mainly clear";
      case 2 -> "Partly cloudy";
      case 3 -> "Overcast";
      case 45, 48 -> "Foggy";
      case 51, 53, 55 -> "Drizzle";
      case 61, 63, 65 -> "Rain";
      case 71, 73, 75 -> "Snow";
      case 80, 81, 82 -> "Rain showers";
      case 95, 96, 99 -> "Thunderstorm";
      default -> "Unknown";
    };
  }
}
