package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.domain.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StrategyRepository extends JpaRepository<Strategy, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByDatabaseTargetId(Long databaseTargetId);

    List<Strategy> findByDatabaseTargetId(Long databaseTargetId);

    List<Strategy> findByActiveTrue();

    // Para el motor de alertas: estrategias activas sin ninguna programación activa
    @Query("""
            select st from Strategy st
            where st.active = true
              and not exists (select s.id from Schedule s where s.strategy = st and s.active = true)
            """)
    List<Strategy> findActiveWithoutActiveSchedule();
}