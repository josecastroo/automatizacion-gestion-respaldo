package com.example.automatizacion_gestion_respaldo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution")
@Getter
@Setter
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule; // Null if manual

    @Column(nullable = false)
    private String origin; // PROGRAMADA, MANUAL

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private Long durationSeconds;

    @Column(nullable = false)
    private String status; // EXITOSO, ERROR, ADVERTENCIA, EN_EJECUCION, OMITIDO

    @Lob
    @Column(name = "used_script")
    private String usedScript;

    @Lob
    @Column(name = "rman_log")
    private String rmanLog;

    @Column(length = 500)
    private String backupLocation;

    @Column
    private Long totalSizeBytes;

    @Lob
    @Column
    private String identifiedErrors;
}
