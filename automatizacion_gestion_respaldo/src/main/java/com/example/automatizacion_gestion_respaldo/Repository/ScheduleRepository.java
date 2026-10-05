package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByStrategyId(Long strategyId);

    // Para el planificador: programaciones activas de estrategias activas
    @Query("""
            select s from Schedule s
            join fetch s.strategy st
            join fetch s.backupType
            where s.active = true and st.active = true
            """)
    List<Schedule> findAllActiveWithStrategy();
}