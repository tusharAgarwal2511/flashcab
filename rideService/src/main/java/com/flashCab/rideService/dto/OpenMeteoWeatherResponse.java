package com.flashCab.rideService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpenMeteoWeatherResponse {

    private Current current;

    @Data
    @NoArgsConstructor
    public static class Current {
        @JsonProperty("temperature_2m")
        private Double temperature2m;

        private Double precipitation;

        private Double rain;

        private Double showers;

        @JsonProperty("weather_code")
        private Integer weatherCode;

        @JsonProperty("wind_speed_10m")
        private Double windSpeed10m;
    }
}
