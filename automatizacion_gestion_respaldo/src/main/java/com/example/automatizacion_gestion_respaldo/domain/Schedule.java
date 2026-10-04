package com.example.automatizacion_gestion_respaldo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "schedule")
@Getter
@Setter
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "backup_type_id")
    private StrategyBackupType backupType;

    @Column(nullable = false)
    private String cronExpression; // CRON or Days/Hour expression

    @Column
    private String windowStart; // Time like 23:00

    @Column
    private String windowEnd; // Time like 04:00

    @Column(nullable = false)
    private Boolean active = true;
}
