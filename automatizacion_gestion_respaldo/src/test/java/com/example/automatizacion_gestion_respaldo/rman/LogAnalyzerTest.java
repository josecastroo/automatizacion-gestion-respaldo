package com.example.automatizacion_gestion_respaldo.rman;

import com.example.automatizacion_gestion_respaldo.domain.ExecutionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogAnalyzerTest {

    private final LogAnalyzer analyzer = new LogAnalyzer();

    private static final String LOG_OK =
            "Recovery Manager: Release 21.0.0.0.0 - Production\n"
                    + "Finished backup at 04-OCT-26\n"
                    + "Recovery Manager complete.\n";

    @Test
    void codigoDeSalidaDistintoDeCero_esError() {
        assertThat(analyzer.determineStatus(1, LOG_OK)).isEqualTo(ExecutionStatus.ERROR);
    }

    @Test
    void logLimpio_esExitoso() {
        assertThat(analyzer.determineStatus(0, LOG_OK)).isEqualTo(ExecutionStatus.EXITOSO);
    }

    @Test
    void logNulo_noLanzaExcepcion_yEsAdvertencia() {
        assertThat(analyzer.determineStatus(0, null)).isEqualTo(ExecutionStatus.ADVERTENCIA);
    }

    @Test
    void pilaDeErrorConCodigoCero_esError() {
        String log = "RMAN-00569: =============== ERROR MESSAGE STACK FOLLOWS ===============\n"
                + "RMAN-03002: failure of backup command\n"
                + "Recovery Manager complete.\n";

        assertThat(analyzer.determineStatus(0, log)).isEqualTo(ExecutionStatus.ERROR);
    }

    @Test
    void codigoRmanSinPilaDeError_esAdvertencia() {
        String log = "RMAN-06207: WARNING: 1 objects could not be deleted\n"
                + "Recovery Manager complete.\n";

        assertThat(analyzer.determineStatus(0, log)).isEqualTo(ExecutionStatus.ADVERTENCIA);
    }

    @Test
    void logSinMarcadorDeFinalizacion_esAdvertencia() {
        assertThat(analyzer.determineStatus(0, "Finished backup at 04-OCT-26\n"))
                .isEqualTo(ExecutionStatus.ADVERTENCIA);
    }

    @Test
    void extractErrors_devuelveCodigosRmanYOra_sinLineasDecorativas() {
        String log = "RMAN-00571: ===========================================================\n"
                + "RMAN-00569: =============== ERROR MESSAGE STACK FOLLOWS ===============\n"
                + "RMAN-03002: failure of backup command\n"
                + "ORA-19504: failed to create file\n";

        assertThat(analyzer.extractErrors(log))
                .isEqualTo("RMAN-03002: failure of backup command\nORA-19504: failed to create file");
    }

    @Test
    void extractErrors_logLimpioONulo_devuelveNull() {
        assertThat(analyzer.extractErrors(LOG_OK)).isNull();
        assertThat(analyzer.extractErrors(null)).isNull();
    }
}