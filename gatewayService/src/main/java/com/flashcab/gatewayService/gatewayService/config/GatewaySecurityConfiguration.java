package com.flashcab.gatewayService.gatewayService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(@Value("${GATEWAY_JWT_SECRET}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) {
            throw new IllegalStateException("GATEWAY_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        SecretKey secretKey = new SecretKeySpec(key, "HmacSHA256");
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> roleValidator = jwt -> {
            String role = jwt.getClaimAsString("role");
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || role == null || !Set.of("RIDER", "DRIVER").contains(role)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required user identity or role is missing", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), roleValidator));
        return decoder;
    }

    @Bean
    Converter<Jwt, reactor.core.publisher.Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || !Set.of("RIDER", "DRIVER").contains(role)) {
                return List.of();
            }
            return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    @Bean
    SecurityWebFilterChain gatewaySecurityFilterChain(
            ServerHttpSecurity http,
            Converter<Jwt, reactor.core.publisher.Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()
                        .pathMatchers("/auth/**", "/internal/**").denyAll()
                        .pathMatchers("/api/v1/drivers/batch").denyAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/locations/drivers/seed").denyAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/locations/drivers/*/reserve",
                                "/api/v1/locations/drivers/*/release").denyAll()
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/locations/drivers/*").denyAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/rides/request").hasRole("RIDER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/rides/rider/**").hasRole("RIDER")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/rides/*/cancel").hasRole("RIDER")
                        .pathMatchers(HttpMethod.PUT,
                                "/api/v1/rides/*/start", "/api/v1/rides/*/complete").hasRole("DRIVER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/rides/*").hasAnyRole("RIDER", "DRIVER")
                        .pathMatchers("/api/v1/rides/**").denyAll()
                        .pathMatchers("/api/v1/riders/**").hasRole("RIDER")
                        .pathMatchers("/api/v1/drivers/**").hasRole("DRIVER")
                        .pathMatchers(HttpMethod.POST, "/api/v1/locations/drivers/update").hasRole("DRIVER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/locations/drivers/nearby").hasRole("RIDER")
                        .pathMatchers("/api/v1/locations/**").denyAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
