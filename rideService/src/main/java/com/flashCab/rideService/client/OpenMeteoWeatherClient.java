package com.flashCab.rideService.client;

import com.flashCab.rideService.dto.OpenMeteoWeatherResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class OpenMeteoWeatherClient {

    private static final String OPEN_METEO_URL = "https://api.open-meteo.com";
    private final RestClient restClient;

    public OpenMeteoWeatherClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(OPEN_METEO_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public OpenMeteoWeatherResponse getCurrentWeather(double latitude, double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam(
                                "current",
                                "temperature_2m,precipitation,rain,showers,weather_code,wind_speed_10m"
                        )
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .body(OpenMeteoWeatherResponse.class);
    }
}
