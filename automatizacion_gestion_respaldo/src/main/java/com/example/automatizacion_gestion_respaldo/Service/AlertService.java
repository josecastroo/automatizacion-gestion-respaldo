package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.DTO.Notice;
import com.example.automatizacion_gestion_respaldo.DTO.NoticeLevel;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.domain.Execution;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import com.example.automatizacion_gestion_respaldo.domain.Strategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private final StrategyRepository strategyRepository;
    private final ExecutionRepository executionRepository;

    public AlertService(StrategyRepository strategyRepository, ExecutionRepository executionRepository) {
        this.strategyRepository = strategyRepository;
        this.executionRepository = executionRepository;
    }

    public List<Notice> getSystemAlerts() {
        List<Notice> notices = new ArrayList<>();
        List<Strategy> activeStrategies = strategyRepository.findByActiveTrue();

        // 1. Alerta: Estrategias activas sin ninguna programación
        List<Strategy> withoutSchedule = strategyRepository.findActiveWithoutActiveSchedule();
        for (Strategy st : withoutSchedule) {
            notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                    "La estrategia '" + st.getName() + "' está activa pero no tiene ninguna programación (Schedule) activa. No se ejecutará automáticamente."));
        }

        for (Strategy strategy : activeStrategies) {
            // 2. Alerta: Base de datos en NOARCHIVELOG
            if (strategy.getDatabaseTarget().getArchiveMode() == DatabaseTarget.ArchiveMode.NOARCHIVELOG) {
                notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                        "La base de datos objetivo para la estrategia '" + strategy.getName() +
                                "' está en modo NOARCHIVELOG. Las opciones de recuperación en el tiempo son limitadas."));
            } else if (strategy.getDatabaseTarget().getArchiveMode() == DatabaseTarget.ArchiveMode.ARCHIVELOG) {
                notices.add(new Notice(NoticeLevel.RECOMENDACION,
                        "La base de datos de '" + strategy.getName() +
                                "' está en ARCHIVELOG. Considere incluir el respaldo de Archived Redo Logs si aún no lo ha hecho."));
            }

            // 3. Alerta: Ejecución fallida reciente
            Optional<Execution> lastExecution = executionRepository.findFirstByStrategyIdOrderByStartTimeDesc(strategy.getId());
            if (lastExecution.isPresent() && lastExecution.get().getStatus() == ExecutionStatus.ERROR) {
                notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                        "La última ejecución de la estrategia '" + strategy.getName() +
                                "' falló. Revise el historial para más detalles."));
            }

            // 4. Alerta: Estrategia sin respaldo reciente (ej. más de 48 horas sin un EXITOSO o ADVERTENCIA)
            Optional<Execution> lastGoodExecution = executionRepository.findFirstByStrategyIdAndStatusInOrderByStartTimeDesc(
                    strategy.getId(), List.of(ExecutionStatus.EXITOSO, ExecutionStatus.ADVERTENCIA));

            if (lastGoodExecution.isEmpty()) {
                notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                        "La estrategia '" + strategy.getName() + "' nunca ha completado un respaldo exitoso."));
            } else {
                LocalDateTime threshold = LocalDateTime.now().minusHours(48); // Configurable según el negocio
                if (lastGoodExecution.get().getStartTime().isBefore(threshold)) {
                    notices.add(new Notice(NoticeLevel.ADVERTENCIA,
                            "La estrategia '" + strategy.getName() + "' no ha tenido un respaldo exitoso en las últimas 48 horas."));
                }
            }
        }

        return notices;
    }
}