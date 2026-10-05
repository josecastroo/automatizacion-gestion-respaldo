package com.example.automatizacion_gestion_respaldo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "strategy_component")
@Getter
@Setter
public class StrategyComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType componentType;

    // TABLESPACE: nombre. DATAFILE: número. Nulo para DB, CONTROLFILE, SPFILE y ARCHIVELOG.
    @Pattern(regexp = "^[A-Za-z0-9_$#]{1,30}$")
    @Column(length = 30)
    private String objectName;

    @Enumerated(EnumType.STRING)
    private Priority priority;
}