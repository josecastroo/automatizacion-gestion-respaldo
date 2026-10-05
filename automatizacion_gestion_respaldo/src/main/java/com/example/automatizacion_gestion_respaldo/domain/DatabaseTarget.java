package com.example.automatizacion_gestion_respaldo.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

@Entity
@Table(name = "database_target")
@Getter
@Setter
public class DatabaseTarget {

    public enum ArchiveMode { ARCHIVELOG, NOARCHIVELOG, DESCONOCIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String host;

    @NotNull @Min(1) @Max(65535)
    @Column(nullable = false)
    private Integer port;

    @NotBlank
    @Column(nullable = false)
    private String serviceName;

    @NotBlank
    @Column(nullable = false)
    private String dbUser;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String encryptedCredential;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArchiveMode archiveMode = ArchiveMode.DESCONOCIDO;

    @Column(nullable = false)
    private Boolean active = true;
}