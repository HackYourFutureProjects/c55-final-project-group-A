package nl.hackyourfuture.project.backend.weather;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "Weather", description = "Weather forecast for event locations via external weather service")
public class WeatherController {

  private final WeatherService weatherService;

  @GetMapping
  @Operation(
      summary = "Get weather forecast for a specific location and time",
      description = """
          Returns normalized weather information for the given coordinates and timestamp.
                              The backend automatically converts the event time into the local timezone
                              of the location (using Open-Meteo's timezone=auto).
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "Weather forecast successfully retrieved"
  )
  @ApiResponse(
      responseCode = "400",
      description = "Missing required request parameter",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "503",
      description = "The external weather service is temporarily unavailable",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  public WeatherResponse getWeather(
      @Parameter(
          description = "Latitude of the event location",
          example = "52.366"
      )
      @RequestParam BigDecimal latitude,

      @Parameter(
          description = "Longitude of the event location",
          example = "4.901"
      )
      @RequestParam BigDecimal longitude,

      @Parameter(
          description = """
                    Event time in ISO-8601 format.  
                    This timestamp will be converted into the local timezone of the
                    location before matching it against the hourly forecast.
                    """,
          example = "2026-08-31T15:00:00+02:00"
      )
      @RequestParam OffsetDateTime time
  ){
    return weatherService.getWeather(latitude, longitude, time);
  }
}
