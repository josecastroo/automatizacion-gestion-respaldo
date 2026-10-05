package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.domain.ExecutionFile;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface ExecutionFileRepository extends Repository<ExecutionFile, Long> {

    <S extends ExecutionFile> S save(S file);

    List<ExecutionFile> findByExecutionIdOrderByIdAsc(Long executionId);
}