package com.example.automatizacion_gestion_respaldo.DTO;

import com.example.automatizacion_gestion_respaldo.domain.Priority;
import jakarta.validation.constraints.*;

public record DatabaseTargetRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{1,255}$", message = "Host inválido") String host,
        @NotNull @Min(1) @Max(65535) Integer port,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._$#-]{1,128}$", message = "Servicio inválido") String serviceName,
        @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_$#]{0,127}$", message = "Usuario inválido") String dbUser,
        @Pattern(regexp = "^[^\"\\r\\n]*$",
                message = "La contraseña no puede contener comillas dobles ni saltos de línea") String password,
        @NotNull Priority priority,
        Boolean active
) {}