package com.example.automatizacion_gestion_respaldo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "database_target")
@Getter
@Setter
public class DatabaseTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String dbUser;

    @Column(nullable = false)
    private String encryptedCredential;

    @Column(nullable = false)
    private String priority; // ALTA, MEDIA, BAJA

    @Column(nullable = false)
    private Boolean active = true;
}
