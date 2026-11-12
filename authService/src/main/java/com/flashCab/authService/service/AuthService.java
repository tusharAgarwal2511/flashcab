package com.flashCab.authService.service;

import com.flashCab.authService.client.DriverProfileClient;
import com.flashCab.authService.client.DriverProfileProvisionRequest;
import com.flashCab.authService.client.ProfileResponse;
import com.flashCab.authService.client.RiderProfileClient;
import com.flashCab.authService.client.RiderProfileProvisionRequest;
import com.flashCab.authService.dto.AuthResponse;
import com.flashCab.authService.dto.LoginRequest;
import com.flashCab.authService.dto.SignupRequest;
import com.flashCab.authService.dto.SignupResponse;
import com.flashCab.authService.model.AccountStatus;
import com.flashCab.authService.model.AuthUser;
import com.flashCab.authService.model.UserRole;
import com.flashCab.authService.repository.AuthUserRepository;
import feign.FeignException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RiderProfileClient riderProfileClient;
    private final DriverProfileClient driverProfileClient;

    @Value("${auth.jwt.expiration-seconds}")
    private long expirationSeconds;

    @PostConstruct
    void validateTokenExpiration() {
        if (expirationSeconds <= 0) {
            throw new IllegalStateException("AUTH_JWT_EXPIRATION_SECONDS must be greater than zero");
        }
    }

    public SignupResponse signup(SignupRequest request) {
        validateSignup(request);
        String email = normalizeEmail(request.email());
        AuthUser user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if (user.getStatus() != AccountStatus.PENDING_PROFILE
                    || user.getRole() != request.role()
                    || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
            }
        } else {
            user = createPendingUser(request, email);
        }

        ProfileResponse profile = provisionProfile(user);
        if (profile == null || profile.id() == null || profile.id().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile service returned an invalid response; retry signup");
        }

        user.setProfileId(profile.id());
        user.setStatus(AccountStatus.ACTIVE);
        AuthUser activeUser = userRepository.save(user);
        return new SignupResponse(activeUser.getId(), activeUser.getProfileId(),
                activeUser.getEmail(), activeUser.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AuthUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getStatus() != AccountStatus.ACTIVE
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new AuthResponse(token, "Bearer", expirationSeconds, expiresAt,
                user.getId(), user.getProfileId(), user.getRole());
    }

    private AuthUser createPendingUser(SignupRequest request, String email) {
        AuthUser user = new AuthUser();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(AccountStatus.PENDING_PROFILE);
        user.setName(request.name().trim());
        user.setPhone(request.phone().trim());
        user.setVehicleType(request.vehicleType());
        user.setVehicleNumber(request.vehicleNumber());
        user.setCreatedAt(Instant.now());
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
    }

    private ProfileResponse provisionProfile(AuthUser user) {
        try {
            if (user.getRole() == UserRole.RIDER) {
                return riderProfileClient.provisionRider(new RiderProfileProvisionRequest(
                        user.getId(), user.getName(), user.getEmail(), user.getPhone()));
            }
            return driverProfileClient.provisionDriver(new DriverProfileProvisionRequest(
                    user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                    user.getVehicleType(), user.getVehicleNumber()));
        } catch (FeignException exception) {
            log.error("Profile provisioning failed for auth user {} with status {}",
                    user.getId(), exception.status());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile service is unavailable; retry signup");
        }
    }

    private void validateSignup(SignupRequest request) {
        if (request.role() == UserRole.DRIVER
                && (request.vehicleType() == null
                || request.vehicleNumber() == null
                || request.vehicleNumber().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Driver signup requires vehicleType and vehicleNumber");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
