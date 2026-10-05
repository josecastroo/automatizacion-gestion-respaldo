package com.example.automatizacion_gestion_respaldo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "execution_file")
@Getter
@Setter
public class ExecutionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id")
    private Execution execution;

    @Column(nullable = false, length = 500)
    private String path;

    private Long sizeBytes;
}