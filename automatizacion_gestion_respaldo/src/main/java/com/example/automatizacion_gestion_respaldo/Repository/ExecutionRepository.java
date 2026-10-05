package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.Repository.StatusCount;
import com.example.automatizacion_gestion_respaldo.domain.Execution;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Extiende Repository (no JpaRepository) a propósito: las ejecuciones son evidencia
 * y no deben poder borrarse. Solo se guardan, se consultan y se archivan.
 */
public interface ExecutionRepository extends Repository<Execution, Long>, JpaSpecificationExecutor<Execution> {

    <S extends Execution> S save(S execution);

    Optional<Execution> findById(Long id);

    List<Execution> findByStatus(ExecutionStatus status);

    boolean existsByStrategyId(Long strategyId);

    boolean existsByStrategyIdAndStatus(Long strategyId, ExecutionStatus status);

    Optional<Execution> findFirstByStrategyIdOrderByStartTimeDesc(Long strategyId);

    // Último respaldo "bueno" de una estrategia (alerta de "sin respaldo reciente")
    Optional<Execution> findFirstByStrategyIdAndStatusInOrderByStartTimeDesc(
            Long strategyId, Collection<ExecutionStatus> statuses);

    // Para el planificador: ¿esta programación ya generó una ejecución desde ese momento?
    boolean existsByScheduleIdAndStartTimeGreaterThanEqual(Long scheduleId, LocalDateTime since);

    // Resumen por período: cuántas ejecuciones hubo por estado
    @Query("""
            select e.status as status, count(e) as total
            from Execution e
            where e.startTime >= :from and e.startTime < :to and e.archived = false
            group by e.status
            """)
    List<StatusCount> summaryByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}