package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatabaseTargetRepository extends JpaRepository<DatabaseTarget, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<DatabaseTarget> findByActiveTrue();
}