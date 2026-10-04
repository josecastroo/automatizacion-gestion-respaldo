# Sistema de Gestión de Estrategias de Respaldo para Oracle (RMAN)

> Especificación funcional del software a implementar. Las tecnologías y la estructura del proyecto las define el proyecto ya creado: respétalas y no las cambies.
> Contexto: curso EIF402 Administración de Bases de Datos, Universidad Nacional (II ciclo 2026).

## 1. Objetivo

Construir una **herramienta de automatización y gestión de respaldos** para bases de datos Oracle. RMAN es el motor técnico de ejecución. La herramienta es la **capa de gestión**: define estrategias, las programa, ejecuta scripts RMAN y registra evidencia verificable de cada ejecución.

Flujo central (no lo alteres):

```
Estrategia -> programación -> script RMAN -> ejecución -> resultado -> evidencia
```

Principio de diseño clave: **separar la estrategia de la ejecución**. La estrategia guarda las decisiones (qué, cómo, cuándo). RMAN ejecuta. Cambiar una estrategia no debe obligar a editar scripts a mano: el script se **genera** a partir de la estrategia.

## 2. Modelo conceptual: Qué / Cómo / Cuándo

### 2.1 Qué respaldar (alcance y prioridad)
Componentes posibles:
- Base de datos completa
- Tablespaces
- Datafiles
- Control files
- SPFILE
- Archived redo logs

Cada base de datos o componente tiene una **prioridad**: `ALTA`, `MEDIA` o `BAJA`.
- Alta: su pérdida detiene o afecta la operación (ej. producción). Respaldo frecuente y recuperación bien definida.
- Media: importante, pero la pérdida temporal es tolerable.
- Baja: reconstruible o de impacto menor (ej. base de pruebas).
- La prioridad **no determina por sí sola** el tipo de respaldo; es un criterio para elegir frecuencia, modalidad y nivel de protección.

### 2.2 Cómo respaldar (tipo de respaldo)
| Tipo | Descripción |
|---|---|
| `FULL` | Respaldo completo. Copia integral de referencia. Costoso en espacio y tiempo si es muy frecuente. |
| `INC_0` | Incremental nivel 0. Punto de partida de una estrategia incremental. |
| `INC_1_DIF` | Incremental nivel 1 **diferencial**: bloques modificados desde el último incremental (nivel 0 o 1). Respaldos diarios pequeños, la recuperación aplica varios incrementales. |
| `INC_1_ACUM` | Incremental nivel 1 **acumulativo**: bloques modificados desde el último nivel 0. Más grande, pero simplifica la recuperación. |
| `ARCHIVELOG` | Respaldo de archived redo logs. |

La elección balancea: almacenamiento, tiempo de respaldo, tiempo y complejidad de recuperación, frecuencia de cambio de datos y ventana disponible. **No hay un tipo único válido para todo**; debe ser configurable.

### 2.3 Cuándo respaldar (frecuencia y programación)
Debe soportar: días de la semana, hora, intervalos, **ventana de respaldo** (inicio/fin), ejecución manual antes de operaciones críticas y **retención** (cuánto tiempo se conservan los respaldos).

Ejemplo de estrategia típica:
- Nivel 0 una vez por semana
- Nivel 1 diario
- Archived redo logs cada N minutos u horas
- Retención de X días

## 3. Módulos funcionales

1. **Administración de estrategias**: crear, modificar, activar/desactivar, consultar.
2. **Definición de qué respaldar**: seleccionar base de datos y componentes, asignar prioridad.
3. **Definición de cómo respaldar**: tipo de respaldo y parámetros de ejecución.
4. **Definición de cuándo respaldar**: horarios, frecuencia, días, ventana, retención.
5. **Ejecución**: generar y lanzar el script RMAN (programado o manual).
6. **Monitoreo**: estado de operaciones en curso y detección de errores.
7. **Evidencia e historial**: registro completo de cada ejecución.
8. **Recuperación** (fase posterior): listar respaldos disponibles y apoyar los procedimientos de restauración.

## 4. Evidencia por ejecución (obligatorio)

Cada ejecución debe guardar:
- estrategia ejecutada
- base de datos
- tipo de respaldo
- script utilizado (el texto RMAN generado)
- fecha y hora de inicio y de finalización
- duración
- resultado: `EXITOSO`, `ERROR`, `ADVERTENCIA`, `EN_EJECUCION`
- mensaje o log completo de RMAN (códigos `RMAN-xxxxx` y `ORA-xxxxx`)
- ubicación del respaldo
- archivos generados y tamaño total
- errores o advertencias identificados
- origen: `PROGRAMADA` o `MANUAL`

Vista de historial esperada (ejemplo):

| Fecha | Estrategia | Tipo | Inicio | Fin | Resultado |
|---|---|---|---|---|---|
| 15/09/2026 | Producción diaria | Incremental | 23:00 | 23:38 | Exitoso |
| 17/09/2026 | Producción diaria | Incremental | 23:00 | 23:12 | Error |

Reglas:
- Una ejecución **nunca se borra**; es evidencia. Solo se puede archivar.
- Si el planificador debía ejecutar un respaldo y no lo hizo, debe registrarse como **omitido**, no quedar en silencio.
- El historial se filtra por fecha, base de datos, estrategia y resultado, y se exporta a CSV y PDF.
- Debe existir un detalle por ejecución (script, log, tamaño, ubicación, duración) y un resumen por período (exitosos vs. fallidos).

## 5. Componentes lógicos

```
Gestión de estrategias -> Planificador -> Generador de scripts RMAN
                                              |
                                              v
                              Ejecutor RMAN -> Analizador de log -> Evidencia
```

- **Generador de scripts**: construye el script RMAN a partir de la estrategia.
- **Ejecutor RMAN**: ejecuta `rman` en el host Oracle. Debe estar detrás de una abstracción para poder cambiar el mecanismo (local, remoto) y para tener una **implementación simulada**.
- **Analizador de log**: determina el resultado combinando el código de salida de `rman` con la búsqueda de `RMAN-` y `ORA-` en la salida.
- **Ejecutor simulado (dry-run)**: genera logs de ejemplo para desarrollar y probar sin una instancia Oracle con RMAN.
- **Planificador**: decide qué operación corresponde en cada momento, la activa y detecta ejecuciones omitidas.

## 6. Modelo de datos lógico (mínimo)

- **Base de datos objetivo**: nombre, host, puerto, servicio, usuario, credencial cifrada, prioridad, activa.
- **Estrategia**: nombre, base de datos, activa, retención en días, descripción.
- **Componente de estrategia**: estrategia, tipo (`DB`, `TABLESPACE`, `DATAFILE`, `CONTROLFILE`, `SPFILE`, `ARCHIVELOG`), nombre del objeto, prioridad.
- **Tipo de respaldo de estrategia**: estrategia, tipo (`FULL`, `INC_0`, `INC_1_DIF`, `INC_1_ACUM`, `ARCHIVELOG`), parámetros.
- **Programación**: estrategia, tipo de respaldo, expresión (cron o días/hora), ventana inicio/fin, activa.
- **Ejecución**: estrategia, programación (opcional), origen, inicio, fin, duración, estado, script usado, log RMAN, ubicación, tamaño, errores.
- **Archivo de ejecución**: ejecución, ruta, tamaño.

Una estrategia puede tener varias programaciones (ej. nivel 0 semanal y nivel 1 diario).

Regla de diseño: el repositorio de la herramienta debería poder ubicarse en una base distinta de las que respalda, para no perder la evidencia justo cuando una base falla. Hacerlo configurable.

## 7. Generación de scripts RMAN

Los scripts se construyen desde plantillas según el tipo. Referencia:

```rman
-- FULL
BACKUP DATABASE PLUS ARCHIVELOG;

-- INC_0
BACKUP INCREMENTAL LEVEL 0 DATABASE PLUS ARCHIVELOG;

-- INC_1_DIF (diferencial, por defecto en RMAN)
BACKUP INCREMENTAL LEVEL 1 DATABASE;

-- INC_1_ACUM
BACKUP INCREMENTAL LEVEL 1 CUMULATIVE DATABASE;

-- ARCHIVELOG
BACKUP ARCHIVELOG ALL NOT BACKED UP 1 TIMES;

-- Componentes individuales
BACKUP TABLESPACE nombre;
BACKUP DATAFILE n;
BACKUP CURRENT CONTROLFILE;
BACKUP SPFILE;

-- Retención
CONFIGURE RETENTION POLICY TO RECOVERY WINDOW OF n DAYS;
DELETE NOPROMPT OBSOLETE;
```

Ejecución: `rman target <conexión> cmdfile=<script> log=<log>`

Reglas de seguridad:
- **Nunca** concatenar entrada del usuario sin validar. Validar nombres de tablespace y datafile contra un patrón estricto o una lista permitida.
- No escribir contraseñas en scripts ni en logs. Usar credenciales cifradas, variables de entorno, wallet o autenticación del sistema operativo.
- Guardar el script generado con cada ejecución para auditoría.

## 8. Operaciones que debe exponer el sistema

- Gestionar bases de datos objetivo: crear, listar, editar, eliminar.
- Gestionar estrategias: crear, editar, activar, desactivar, listar, ver detalle.
- Gestionar programaciones de una estrategia.
- Ejecutar una estrategia manualmente.
- Previsualizar el script RMAN que generaría una estrategia, sin ejecutarlo.
- Consultar ejecuciones con filtros (fecha, base de datos, estrategia, estado) y ver el detalle de una.
- Obtener el resumen por período (exitosos vs. fallidos).
- Exportar el historial a CSV y PDF.

## 9. Pantallas mínimas

- **Bases de datos**: listado y formulario.
- **Estrategias**: listado, formulario con qué/cómo/cuándo, activar/desactivar y vista previa del script.
- **Historial**: tabla con filtros, estado visible (exitoso/error) y exportación.
- **Detalle de ejecución**: script, log, tamaño, ubicación, duración, errores.
- **Resumen**: exitosos vs. fallidos por período.

## 10. Fases de desarrollo

**Fase 1 (MVP):**
1. Modelo de datos.
2. Gestión de bases de datos y estrategias (qué/cómo/cuándo).
3. Generador de scripts RMAN con pruebas unitarias.
4. Ejecutor simulado y ejecutor real.
5. Analizador de log y registro de evidencia.
6. Planificador y ejecución manual.
7. Pantallas de estrategias, historial y detalle.
8. Filtros y exportación del historial.

**Fase 2:** notificaciones por error, detección de respaldos omitidos, resumen visual.

**Fase 3:** módulo de recuperación (listar respaldos con `LIST BACKUP`, generar scripts de restauración, validar con `RESTORE ... VALIDATE`).

## 11. Criterios de aceptación

- Se puede definir una estrategia indicando qué, cómo y cuándo, sin escribir RMAN a mano.
- Cambiar la estrategia cambia el script generado sin editar scripts manualmente.
- Una ejecución programada y una manual dejan evidencia completa (sección 4).
- Un error de RMAN queda registrado como `ERROR` con su mensaje.
- El historial se puede filtrar y exportar.
- Todo el flujo funciona con el ejecutor simulado, sin necesidad de RMAN real.
- El generador de scripts y el analizador de log tienen pruebas.

## 12. Fuera de alcance (por ahora)

- Recuperación automática completa (Fase 3).
- Respaldo en la nube o en cintas.
- Alta disponibilidad de la propia herramienta.

## 13. Convenciones

- Idioma de la interfaz, mensajes y documentación: **español**.
- No hardcodear credenciales ni rutas; usar configuración externa.
- Seguir las convenciones y la estructura del proyecto existente.
- Cambios pequeños y bien delimitados por funcionalidad.
