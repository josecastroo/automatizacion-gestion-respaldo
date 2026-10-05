package com.example.automatizacion_gestion_respaldo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Getter
@Setter
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_type_id")
    private StrategyBackupType backupType;

    // Cron de Spring de 6 campos: seg min hora día mes díaSemana (ej. "0 0 23 * * *")
    @NotBlank
    @Column(nullable = false)
    private String cronExpression;

    private LocalTime windowStart;
    private LocalTime windowEnd;

    @Column(nullable = false)
    private Boolean active = true;
}