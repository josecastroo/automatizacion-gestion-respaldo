package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;

public interface StatusCount {
    ExecutionStatus getStatus();
    Long getTotal();
}