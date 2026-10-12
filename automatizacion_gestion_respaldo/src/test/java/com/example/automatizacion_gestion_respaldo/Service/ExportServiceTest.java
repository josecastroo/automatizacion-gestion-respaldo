package com.example.automatizacion_gestion_respaldo.Service;

import com.example.automatizacion_gestion_respaldo.DTO.ExecutionSummaryResponse;
import com.example.automatizacion_gestion_respaldo.domain.BackupType;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionOrigin;
import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportServiceTest {

    private final ExportService service = new ExportService();

    private ExecutionSummaryResponse row(String strategyName, ExecutionStatus status) {
        return new ExecutionSummaryResponse(1L,
                LocalDateTime.of(2026, 10, 7, 23, 0, 0), LocalDateTime.of(2026, 10, 7, 23, 38, 0),
                2280L, strategyName, "PROD", BackupType.INC_1_DIF, ExecutionOrigin.PROGRAMADA, status, false);
    }

    private String csv(ExecutionSummaryResponse... rows) {
        return new String(service.toCsv(List.of(rows)), StandardCharsets.UTF_8);
    }

    @Test
    void csv_incluyeEncabezadoYFila() {
        String csv = csv(row("Diaria", ExecutionStatus.EXITOSO));

        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("Estrategia", "Base de datos");
        assertThat(csv).contains("07/10/2026", "23:00:00", "INC_1_DIF", "PROGRAMADA", "EXITOSO", "2280");
    }

    @Test
    void csv_escapaComasYComillas() {
        String csv = csv(row("Prod, \"semanal\"", ExecutionStatus.EXITOSO));

        assertThat(csv).contains("\"Prod, \"\"semanal\"\"\"");
    }

    @Test
    void csv_neutralizaFormulasDeExcel() {
        String csv = csv(row("=HYPERLINK(\"http://x\")", ExecutionStatus.ERROR));

        assertThat(csv).contains("'=HYPERLINK");
        assertThat(csv).doesNotContain(",=HYPERLINK").doesNotContain(",\"=HYPERLINK");
    }

    @Test
    void pdf_generaUnDocumentoValido_conYSinFilas() {
        byte[] withRows = service.toPdf(List.of(row("Diaria", ExecutionStatus.EXITOSO),
                row("Semanal", ExecutionStatus.ERROR)), "Filtros: desde 2026-10-01");
        byte[] empty = service.toPdf(List.of(), "");

        assertThat(new String(withRows, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        assertThat(new String(empty, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        assertThat(withRows.length).isGreaterThan(empty.length);
    }
}