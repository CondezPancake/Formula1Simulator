# Estado de épicas e historias de usuario

Este documento registra el estado actual del proyecto frente a las épicas e historias definidas en `docs/legacy/ProyectoFormula1.md`.

Estados utilizados:

- **Terminada:** la funcionalidad solicitada está implementada en el proyecto actual.
- **En desarrollo:** la funcionalidad está parcial o todavía no está implementada. Se agrupan ambos casos porque se solicitaron únicamente estos dos estados.

Una épica se considera **Terminada** solamente cuando todas sus historias están terminadas.

## Resumen de épicas

| Épica | Nombre | Estado |
|---|---|---|
| E01 | Gestión de datos de Fórmula 1 | **Terminada** |
| E02 | Configuración de la simulación | **Terminada** |
| E03 | Motor de simulación | **Terminada** |
| E04 | Visualización de resultados | **En desarrollo** |
| E05 | Integración y persistencia | **En desarrollo** |
| E06 | Motor probabilístico de simulación | **Terminada** |
| E07 | Clima dinámico | **En desarrollo** |
| E08 | Sistema de eventos aleatorios | **En desarrollo** |
| E09 | Telemetría visual | **En desarrollo** |
| E10 | Evolución dinámica de pista | **En desarrollo** |
| E11 | Sistema de estrategia | **Terminada** |
| E12 | Análisis automático de la sesión | **En desarrollo** |

## E01 — Gestión de datos de Fórmula 1

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-01 | Gestionar equipos | **Terminada** |
| HU-02 | Gestionar pilotos | **Terminada** |
| HU-03 | Gestionar vehículos | **Terminada** |
| HU-04 | Gestionar circuitos | **Terminada** |
| HU-05 | Consultar información | **Terminada** |
| HU-06 | Comparar vehículos | **Terminada** |

## E02 — Configuración de la simulación

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-07 | Seleccionar circuito | **Terminada** |
| HU-08 | Seleccionar piloto y vehículo | **Terminada** |
| HU-09 | Configurar conducción | **Terminada** |
| HU-10 | Configurar aerodinámica | **Terminada** |
| HU-11 | Configurar neumáticos | **Terminada** |
| HU-12 | Configurar combustible | **Terminada** |
| HU-13 | Guardar configuración | **Terminada** |

## E03 — Motor de simulación

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-14 | Obtener condiciones climáticas | **Terminada** |
| HU-15 | Calcular rendimiento | **Terminada** |
| HU-16 | Calcular tiempo de vuelta | **Terminada** |
| HU-17 | Ejecutar clasificación | **Terminada** |
| HU-18 | Generar clasificación | **Terminada** |
| HU-19 | Mostrar evolución | **Terminada** |

## E04 — Visualización de resultados

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-20 | Visualizar clasificación | **Terminada** |
| HU-21 | Visualizar estadísticas | **En desarrollo** |
| HU-22 | Visualizar clima | **Terminada** |
| HU-23 | Visualizar telemetría | **En desarrollo** |

## E05 — Integración y persistencia

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-24 | Consumir OpenF1 | **En desarrollo** |
| HU-25 | Guardar datos | **Terminada** |
| HU-26 | Guardar resultados | **Terminada** |
| HU-27 | Consultar historial | **Terminada** |

## E06 — Motor probabilístico de simulación

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-28 | Simulación probabilística | **Terminada** |

## E07 — Clima dinámico

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-29 | Evolución climática | **En desarrollo** |

## E08 — Sistema de eventos aleatorios

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-30 | Generar eventos | **En desarrollo** |

## E09 — Telemetría visual

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-31 | Visualizar telemetría | **En desarrollo** |
| HU-32 | Visualizar evolución de la vuelta | **En desarrollo** |
| HU-33 | Comparar sectores | **En desarrollo** |

## E10 — Evolución dinámica de pista

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-34 | Evolución del grip | **En desarrollo** |

## E11 — Sistema de estrategia

**Estado de la épica: Terminada**

| Historia | Nombre | Estado |
|---|---|---|
| HU-35 | Seleccionar estrategia | **Terminada** |

## E12 — Análisis automático de la sesión

**Estado de la épica: En desarrollo**

| Historia | Nombre | Estado |
|---|---|---|
| HU-36 | Generar análisis | **En desarrollo** |

## Totales

| Elemento | Terminadas | En desarrollo | Total |
|---|---:|---:|---:|
| Épicas | 5 | 7 | 12 |
| Historias de usuario | 26 | 10 | 36 |

## Observaciones de alcance

- El alcance vigente descrito por el README es `f1project.md`; `ProyectoFormula1.md` se conserva dentro de `docs/legacy` y contiene funcionalidades adicionales.
- OpenF1, Q1/Q2/Q3 y la telemetría avanzada fueron retirados del alcance actual según el historial del README.
- La simulación actual genera un clima para toda la sesión, pero no lo modifica durante ella; por eso HU-14 está terminada y HU-29 continúa en desarrollo.
- La evolución básica de velocidad, consumo y desgaste está implementada. La telemetría avanzada y los datos por sector de HU-31 a HU-33 continúan en desarrollo.
