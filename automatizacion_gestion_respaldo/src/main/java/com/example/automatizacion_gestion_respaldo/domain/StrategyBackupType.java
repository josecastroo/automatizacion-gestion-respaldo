package com.example.automatizacion_gestion_respaldo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "strategy_backup_type")
@Getter
@Setter
public class StrategyBackupType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @Column(nullable = false)
    private String backupType; // FULL, INC_0, INC_1_DIF, INC_1_ACUM, ARCHIVELOG

    @Column(length = 255)
    private String parameters; // Additional parameters if needed
}
