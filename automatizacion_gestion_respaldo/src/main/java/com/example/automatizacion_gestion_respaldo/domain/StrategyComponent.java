package com.example.automatizacion_gestion_respaldo.domain;

import jakarta.persistence.*;
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    @Column(nullable = false)
    private String componentType; // DB, TABLESPACE, DATAFILE, CONTROLFILE, SPFILE, ARCHIVELOG

    @Column
    private String objectName; // Name of tablespace or datafile, nullable for DB/SPFILE

    @Column
    private String priority; // ALTA, MEDIA, BAJA
}
