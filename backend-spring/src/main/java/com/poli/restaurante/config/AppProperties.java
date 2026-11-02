package com.poli.restaurante.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración tipada de la aplicación (prefijo "app"). Los valores sensibles llegan desde .env. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String timezone,
        @Valid @NotNull Jwt jwt,
        @NotNull Cors cors,
        @NotNull InitialAdmin initialAdmin,
        @Valid @NotNull Login login,
        @Valid @NotNull Storage storage) {

    public record Storage(
            @NotBlank @Pattern(regexp = "local|db|r2", message = "STORAGE_PROVIDER debe ser local, db o r2") String provider,
            @NotBlank String uploadsDir,
            @Valid @NotNull R2 r2) {

        public record R2(String endpoint, String bucket, String accessKeyId, String secretAccessKey) {
        }
    }

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres") String secret,
            @Min(1) long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record InitialAdmin(String name, String email, String password) {
    }

    public record Login(@Min(1) int maxAttempts, @Min(1) int lockMinutes) {
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }
}
