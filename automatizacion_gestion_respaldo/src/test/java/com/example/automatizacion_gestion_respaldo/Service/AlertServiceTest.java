package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.DTO.Notice;
import com.example.automatizacion_gestion_respaldo.DTO.NoticeLevel;
import com.example.automatizacion_gestion_respaldo.Repository.ExecutionRepository;
import com.example.automatizacion_gestion_respaldo.Repository.StrategyRepository;
import com.example.automatizacion_gestion_respaldo.domain.DatabaseTarget;
import com.example.automatizacion_gestion_respaldo.domain.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

    @Mock
    private StrategyRepository strategyRepository;

    @Mock
    private ExecutionRepository executionRepository;

    @InjectMocks
    private AlertService alertService;

    private Strategy strategy;

    @BeforeEach
    void setUp() {
        DatabaseTarget db = new DatabaseTarget();
        db.setArchiveMode(DatabaseTarget.ArchiveMode.NOARCHIVELOG);

        strategy = new Strategy();
        strategy.setId(1L);
        strategy.setName("Estrategia Prueba");
        strategy.setDatabaseTarget(db);
        strategy.setActive(true);
    }

    @Test
    void debeRetornarAlertasPorFaltaDeProgramacionYModoNoArchiveLog() {
        // Simulamos que la base de datos retorna nuestra estrategia de prueba
        when(strategyRepository.findByActiveTrue()).thenReturn(List.of(strategy));
        when(strategyRepository.findActiveWithoutActiveSchedule()).thenReturn(List.of(strategy));

        // Ejecutamos el método
        List<Notice> notices = alertService.getSystemAlerts();

        // Validamos que se generen las 3 alertas esperadas
        assertEquals(3, notices.size());

        // Verificamos el nivel de cada alerta
        assertTrue(notices.stream().allMatch(n -> n.level() == NoticeLevel.ADVERTENCIA));

        // Verificamos los mensajes clave
        assertTrue(notices.get(0).message().contains("no tiene ninguna programación"));
        assertTrue(notices.get(1).message().contains("NOARCHIVELOG"));
        assertTrue(notices.get(2).message().contains("nunca ha completado un respaldo exitoso"));
    }
}