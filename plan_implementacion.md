# Plan de Implementación y Contexto de Arquitectura
**Sistema de Gestión de Estrategias de Respaldo para Oracle (RMAN)**

Este documento sirve como hoja de ruta y contexto técnico para el equipo de desarrollo. Define las herramientas, la arquitectura base y los pasos de implementación.

## 1. Stack Tecnológico y Herramientas

- **Backend (API REST):** Java 17+ con **Spring Boot**.
  - **Persistencia:** Spring Data JPA / Hibernate.
  - **Base de Datos:** Oracle Database.
  - **Cifrado de Credenciales:** Jasypt (Java Simplified Encryption) para Spring Boot.
  - **Planificador de Tareas:** Spring `@Scheduled` nativo.
- **Frontend:** **React** (creado con Vite o Create React App).
  - Comunicación con Backend: `fetch` o `axios` llamando a la API REST de Spring.

## 2. Contexto de Arquitectura Lógica

El proyecto se divide estrictamente para mantener el backend como una API limpia que gestiona la lógica de RMAN y los datos, mientras React se encarga puramente de la presentación.

*   `domain`: Entidades JPA que se mapean a las tablas de Oracle.
*   `repository`: Interfaces Spring Data.
*   `service`: Lógica de negocio y orquestación.
*   `controller`: Endpoints REST (`@RestController`) que consumirá React.
*   `rman`: El corazón del sistema (Generador de scripts RMAN, Ejecutores simulados/reales, Analizador de logs).
*   `scheduler`: Procesos en segundo plano de Spring para revisar programaciones.

## 3. Plan de Implementación Paso por Paso

### FASE INICIAL

*   **Paso 1: Configuración del Proyecto Backend.**
    *   Limpieza del `pom.xml` actual e inyección de dependencias (Web, JPA, Oracle, Jasypt).
    *   Configuración del archivo `application.properties` para la conexión a Oracle (con auto-generación del esquema base).
*   **Paso 2: Inicialización del Proyecto Frontend (React).**
    *   Crear la estructura del proyecto React dentro del mismo repositorio o de forma paralela.
*   **Paso 3: Creación del Modelo de Datos (JPA).**
    *   Codificar las entidades principales: `DatabaseTarget`, `Strategy`, `Schedule`, `Execution`. Hibernate generará las tablas en Oracle al arrancar.
*   **Paso 4: Implementación del Motor RMAN (El Core).**
    *   Crear `RmanScriptBuilder` para generar los scripts basados en las reglas de negocio.
    *   Crear las interfaces de ejecución y el `MockRmanExecutor` (dry-run).
*   **Paso 5: Definición de Contratos API REST.**
    *   Crear los controladores (`@RestController`) básicos con datos simulados o vacíos, que definan las URL que el equipo de frontend usará (ej: `GET /api/strategies`, `POST /api/databases`).
*   **Paso 6: Implementación Lógica de Servicios (CRUD completos).**
    *   Conectar los Controladores REST con la Base de Datos a través de los Repositorios y Servicios.
*   **Paso 7: Desarrollo de Pantallas en React.**
    *   Crear los componentes visuales: Listado de BDs, Formulario de Estrategias, Historial.
    *   Conectar React a la API REST desarrollada en el Paso 6.
*   **Paso 8: Implementación del Planificador (`@Scheduled`).**
    *   Codificar la clase que lee la tabla de programaciones periódicamente y lanza ejecuciones usando el motor RMAN ya construido.
*   **Paso 9: Historial y Analizador de Logs.**
    *   Crear la lógica que guarda los logs resultantes de RMAN en la base de datos para la evidencia.
*   **Paso 10: Pruebas con Oracle y Ejecutor Real.**
    *   Reemplazar el `MockRmanExecutor` por el ejecutor real que llama al sistema operativo en el servidor de base de datos.
