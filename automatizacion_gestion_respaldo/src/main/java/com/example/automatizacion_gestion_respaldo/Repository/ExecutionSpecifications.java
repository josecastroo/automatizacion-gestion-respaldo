package com.example.automatizacion_gestion_respaldo.Repository;

import com.example.automatizacion_gestion_respaldo.domain.Execution;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ExecutionSpecifications {

    private ExecutionSpecifications() {}

    /** Todos los parámetros son opcionales: los nulos se ignoran. */
    public static Specification<Execution> filter(LocalDateTime from, LocalDateTime to,
                                                  String databaseName, Long strategyId,
                                                  ExecutionStatus status, Boolean archived) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("startTime"), from));
            if (to != null) p.add(cb.lessThan(root.<LocalDateTime>get("startTime"), to));
            if (databaseName != null && !databaseName.isBlank()) p.add(cb.equal(root.get("databaseName"), databaseName));
            if (strategyId != null) p.add(cb.equal(root.get("strategy").get("id"), strategyId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (archived != null) p.add(cb.equal(root.get("archived"), archived));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}