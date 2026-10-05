package com.example.automatizacion_gestion_respaldo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "execution", indexes = {
        @Index(name = "idx_execution_start", columnList = "start_time"),
        @Index(name = "idx_execution_status", columnList = "status")
})
@Getter
@Setter
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule; // nulo si es manual

    // Copia de los datos al momento de ejecutar (la evidencia no debe cambiar)
    @Column(nullable = false)
    private String strategyName;

    @Column(nullable = false)
    private String databaseName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BackupType backupType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionOrigin origin;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Lob
    @Column(name = "used_script")
    private String usedScript;

    @Lob
    @Column(name = "rman_log")
    private String rmanLog;

    @Column(length = 500)
    private String backupLocation;

    private Long totalSizeBytes;

    @Lob
    @Column(name = "identified_errors")
    private String identifiedErrors; // errores y advertencias (RMAN-xxxxx / ORA-xxxxx)

    @Column(nullable = false)
    private Boolean archived = false;

    @OneToMany(mappedBy = "execution", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ExecutionFile> files = new ArrayList<>();

    public void addFile(ExecutionFile f) { files.add(f); f.setExecution(this); }
}