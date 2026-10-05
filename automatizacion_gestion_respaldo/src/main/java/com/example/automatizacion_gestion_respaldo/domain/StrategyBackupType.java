package com.example.automatizacion_gestion_respaldo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "strategy_backup_type",
        uniqueConstraints = @UniqueConstraint(columnNames = {"strategy_id", "backup_type"}))
@Getter
@Setter
public class StrategyBackupType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "backup_type", nullable = false)
    private BackupType backupType;

    @Column(nullable = false)
    private Boolean compressed = false;

    @Column(length = 255)
    private String parameters;
}