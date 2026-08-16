# ORDEN MAESTRO DEL PROYECTO
> **Objetivo de esta versión:** organizar toda la información existente sin eliminar requisitos, historias, ideas, arquitectura ni detalles técnicos. El documento conserva el contenido original, pero ahora está ordenado para que el equipo pueda distinguir qué construir primero, qué integra el diferencial y qué queda como ampliación.
## 0\. Regla principal de priorización
El proyecto debe entenderse en **cuatro niveles**, no como una lista donde lo "opcional" se elimina:
### 🔴 NIVEL 1 — MVP FUNCIONAL
Es la base mínima que debe existir para que la simulación funcione de extremo a extremo.

- Gestión de equipos, pilotos, vehículos y circuitos.
- Configuración de una simulación.
- Motor de rendimiento.
- Cálculo de vueltas.
- Ejecución de clasificación.
- Clasificación final.
- Visualización de resultados.
- Estadísticas y clima.
- OpenF1.
- MongoDB.
- Guardado de resultados.
### 🔴 NIVEL 2 — MVP DIFERENCIAL
**No debe tratarse como un bonus.** Estas son las funcionalidades que hacen que el proyecto deje de ser un CRUD con una fórmula y se convierta en una simulación destacable.

- Motor probabilístico.
- Clima dinámico.
- Eventos aleatorios.
- Telemetría visual.
### 🟠 NIVEL 3 — CALIDAD Y PROFUNDIZACIÓN
Se implementa después de que los niveles 1 y 2 estén funcionando.

- Evolución de vueltas.
- Comparación de sectores.
- Evolución dinámica de pista.
- Sistema de estrategia.
- Análisis automático.
- Consulta general.
- Comparación de vehículos.
- Guardado de configuración.
- Evolución.
- Historial.
### 🟢 NIVEL 4 — BONUS
- Telemetría avanzada como ampliación adicional, si queda tiempo después de integrar correctamente la telemetría del nivel 2.
## 1\. Orden recomendado de construcción
~~~ text
FASE 1
Modelos + MongoDB + datos base
        ↓
FASE 2
Configuración de simulación
        ↓
FASE 3
Motor de simulación básico
        ↓
FASE 4
Clasificación Q1 → Q2 → Q3
        ↓
FASE 5
Motor probabilístico
        ↓
FASE 6
Clima dinámico + evolución de pista
        ↓
FASE 7
Eventos aleatorios
        ↓
FASE 8
Telemetría visual
        ↓
FASE 9
Dashboard + estadísticas + análisis
        ↓
FASE 10
Historial + extras + pulido
~~~
## 2\. Regla de arquitectura
La aplicación debe mantener separadas las responsabilidades:
~~~ text
UI
Controllers
Services
Models
Repositories
Database
API
Simulation
Strategy
Events
Telemetry
Utils
~~~

La interfaz no debe contener la lógica principal de simulación, MongoDB ni OpenF1.
## 3\. Regla de POO
Los requisitos de POO, excepciones, concurrencia, lambdas, streams, sobrecarga, getters/setters, `this`, packages, interfaces, herencia y polimorfismo son **requisitos transversales del proyecto**.

No son funcionalidades que deban construirse al final: deben aplicarse mientras se implementan las funcionalidades.
## 4\. Regla para las funcionalidades opcionales
"Opcional" significa **posterior en prioridad**, no "innecesaria".

Si una funcionalidad del nivel 3 no alcanza a implementarse, no debe comprometer:

1. El MVP funcional.
1. El MVP diferencial.
1. La arquitectura POO.
1. La estabilidad del motor.
1. La correcta visualización de resultados.
## 5\. Regla de integración
Las funcionalidades avanzadas deben modificar realmente el resultado de la simulación.

Ejemplo:
~~~ text
CLIMA
  ↓
GRIP
  ↓
NEUMÁTICOS
  ↓
DESGASTE
  ↓
RENDIMIENTO
  ↓
TIEMPO
  ↓
CLASIFICACIÓN
~~~

Los eventos y la telemetría no deben ser únicamente elementos visuales: deben conectarse con el motor cuando su definición indique que tienen impacto sobre el comportamiento.

-----
# Proyecto: Simulación de Fórmula 1
## 1\. Objetivo general
Desarrollar una aplicación de escritorio para simular una sesión de **clasificación de Fórmula 1**, permitiendo administrar pilotos, equipos, vehículos y circuitos, configurar las condiciones del vehículo y ejecutar una simulación cuyo resultado dependa de las características del piloto, vehículo, circuito, clima y estrategia seleccionada.

**TECNOLOGIAS**

- **Java 17**
- **JavaFX**
- **MongoDB**
- **OpenF1 API**
- **Maven**
- **JSON**
- **Git/GitHub**
## 2\. Épicas
## ÉPICA E01 — Gestión de datos de Fórmula 1
##### Esta épica agrupa **equipos, pilotos, vehículos y circuitos**.
### HU-01 — Gestionar equipos
**Como administrador**, quiero registrar, consultar, editar y eliminar equipos, **para mantener actualizadas las escuderías disponibles en la simulación.**
### HU-02 — Gestionar pilotos
**Como administrador**, quiero registrar, consultar, editar y eliminar pilotos, **para mantener actualizados los participantes de la simulación.**

Los pilotos tendrán datos como:
~~~
ID
Nombre
Número
Equipo
Rol
Experiencia
Habilidades
~~~
### HU-03 — Gestionar vehículos
**Como administrador**, quiero registrar, consultar, editar y eliminar vehículos, **para mantener actualizados los autos disponibles para la simulación.**

Datos:
~~~
Equipo
Modelo
Motor
Velocidad máxima
Aceleración
Rendimiento
Consumo
Desgaste
~~~
### HU-04 — Gestionar circuitos
**Como administrador**, quiero registrar, consultar, editar y eliminar circuitos, **para mantener actualizadas las pistas disponibles.**

Datos:
~~~
Nombre
País
Longitud
Vueltas
Descripción
Récord
Ganadores
~~~
### HU-05 — Consultar información
**Como usuario**, quiero consultar pilotos, equipos, vehículos y circuitos, **para conocer las opciones disponibles antes de iniciar una simulación.**
### HU-06 — Comparar vehículos
**Como usuario**, quiero comparar las características de diferentes vehículos, **para analizar cuál puede ofrecer un mejor rendimiento.**
> **Esta HU puede quedar como prioridad media.** Si el tiempo no alcanza, no debería comprometer la simulación principal.
## ÉPICA E02 — Configuración de la simulación
Esta es la pantalla donde el usuario prepara su sesión.
### HU-07 — Seleccionar circuito
**Como usuario**, quiero seleccionar un circuito, **para definir la pista donde se realizará la clasificación.**
### HU-08 — Seleccionar piloto y vehículo
**Como usuario**, quiero seleccionar un piloto y su vehículo, **para determinar quién participará en la simulación.**
### HU-09 — Configurar conducción
**Como usuario**, quiero seleccionar entre conducción normal, agresiva o ahorro, **para modificar el comportamiento del vehículo.**
### HU-10 — Configurar aerodinámica
**Como usuario**, quiero seleccionar la carga aerodinámica baja, media o alta, **para adaptar el vehículo al circuito.**
### HU-11 — Configurar neumáticos
**Como usuario**, quiero seleccionar la presión de los neumáticos, **para modificar la tracción y desgaste.**
### HU-12 — Configurar combustible
**Como usuario**, quiero seleccionar una estrategia agresiva, balanceada o de ahorro, **para modificar el consumo y rendimiento del vehículo.**
### HU-13 — Guardar configuración
**Como usuario**, quiero guardar mi configuración de simulación, **para reutilizarla posteriormente.**

Esta última puede implementarse directamente con MongoDB.
## ÉPICA E03 — Motor de simulación
Esta es **la parte más importante del proyecto**.

Aquí es donde dejan de tener un CRUD y realmente tienen una **simulación de Fórmula 1**.
### HU-14 — Obtener condiciones climáticas
**Como sistema**, quiero obtener o generar las condiciones climáticas de la sesión, **para modificar el rendimiento de los vehículos.**

Condiciones:
~~~
☀️ Seco
🌧️ Lluvioso
⛈️ Extremo
~~~

-----
### HU-15 — Calcular rendimiento
**Como sistema**, quiero calcular el rendimiento de cada participante considerando piloto, vehículo, circuito, clima y configuración, **para obtener un comportamiento diferente en cada simulación.**

La fórmula conceptual podría ser:
~~~
RENDIMIENTO


        PILOTO
           +
       VEHÍCULO
           +
       CIRCUITO
           +
         CLIMA
           +
     CONFIGURACIÓN
           ↓
      MODIFICADORES
           ↓
      TIEMPO VUELTA
~~~

-----
### HU-16 — Calcular tiempo de vuelta
**Como sistema**, quiero calcular el tiempo de vuelta de cada piloto, **para determinar su posición en la clasificación.**

-----
### HU-17 — Ejecutar clasificación
**Como usuario**, quiero iniciar una sesión de clasificación, **para simular el rendimiento de los participantes en el circuito seleccionado.**

-----
### HU-18 — Generar clasificación
**Como sistema**, quiero ordenar los pilotos según sus tiempos de vuelta, **para establecer las posiciones finales.**

-----
### HU-19 — Mostrar evolución
**Como usuario**, quiero visualizar indicadores de velocidad, combustible y desgaste de neumáticos durante la simulación, **para observar cómo evoluciona el rendimiento del vehículo.**
## ÉPICA E04 — Visualización de resultados
Aquí es donde **JavaFX realmente tiene que lucirse**.
### HU-20 — Visualizar clasificación
**Como usuario**, quiero visualizar la clasificación final con posición, piloto, equipo y tiempo, **para conocer el resultado de la sesión.**

Ejemplo:
~~~
┌────┬──────────────┬─────────────┬──────────┐───────│
│POS │ PILOTO       │ EQUIPO      │ TIEMPO   │ LLANTA│
├────┼──────────────┼─────────────┼──────────┤───────│
│ 1  │ Verstappen   │ Red Bull    │ 1:10.234 │   S   │
│ 2  │ Leclerc      │ Ferrari     │ 1:10.512 │   M   │
│ 3  │ Norris       │ McLaren     │ 1:10.723 │   H   │
└────┴──────────────┴─────────────┴──────────┘└────┴─│
~~~
### HU-21 — Visualizar estadísticas
**Como usuario**, quiero visualizar gráficamente el rendimiento de los participantes, **para comparar sus resultados.**

Aquí entran:

- `BarChart`
- `LineChart`
- `ProgressBar`
### HU-22 — Visualizar clima
**Como usuario**, quiero visualizar las condiciones climáticas de la sesión, **para conocer cómo influyeron en la clasificación.**
### HU-23 — Visualizar telemetría
**Como usuario**, quiero visualizar datos de telemetría disponibles, **para observar información del comportamiento del vehículo.**

Esta HU la considero **opcional**.

Si el día 3 están justos de tiempo, la eliminan.
## ÉPICA E05 — Integración y persistencia
Esta épica une todo.
### HU-24 — Consumir OpenF1
**Como sistema**, quiero consumir información de OpenF1, **para utilizar datos reales de Fórmula 1 dentro de la aplicación.**

Podemos utilizar:
~~~
Pilotos
Sesiones
Circuitos
Clima
Telemetría
Tiempos
~~~

Pero **no necesitamos utilizar todos los endpoints**.

-----
### HU-25 — Guardar datos
**Como sistema**, quiero almacenar pilotos, equipos, vehículos y circuitos en MongoDB, **para mantener la información disponible entre ejecuciones.**
### HU-26 — Guardar resultados
**Como sistema**, quiero almacenar los resultados de las sesiones de clasificación, **para consultar el historial posteriormente.**
### HU-27 — Consultar historial
**Como usuario**, quiero consultar mis simulaciones anteriores, **para comparar resultados y configuraciones.**

-----
## Scope — Dentro del proyecto
El **scope** debe ser bastante concreto.
## ✅ Dentro del alcance
### Gestión de información
- Equipos.
- Pilotos.
- Vehículos.
- Circuitos.
### Simulación
- Selección de circuito.
- Selección de piloto/vehículo.
- Configuración del vehículo.
- Condiciones climáticas.
- Cálculo de rendimiento.
- Cálculo de tiempos.
- Clasificación.
### Visualización
- Dashboard.
- Tablas.
- Gráficas.
- Indicadores de rendimiento.
- Condiciones climáticas.
- Resultado final.
### Datos
- MongoDB.
- OpenF1.
- Historial básico.
### Tecnologías
- Java 17.
- JavaFX.
- Maven.
- MongoDB.
- OpenF1.
- Jackson.
- Git/GitHub.
  -----
  ## Scope — Fuera del proyecto
## ❌ Fuera del alcance
### Carrera completa
No se implementará una carrera completa con:

- 50+ vueltas.
- Safety Car.
- VSC.
- Banderas.
- Accidentes.
- Adelantamientos físicos.
- IA avanzada de pilotos.

El proyecto se enfoca principalmente en **clasificación/sesión simulada**.

-----
### Simulación 3D
No se desarrollará:

- Motor 3D.
- Circuitos 3D.
- Modelos 3D de vehículos.
- Física avanzada del vehículo.
-----
### Multiplayer
No se implementará:

- Multijugador online.
- Salas.
- Matchmaking.
- Competición entre usuarios.
-----
### Sistema de cuentas
No se implementará:

- Registro de usuarios.
- Login.
- Recuperación de contraseña.
- Roles de usuario reales.

El rol de administrador puede representarse conceptualmente dentro de la aplicación.

-----
### Pagos
No se implementará ningún:

- Sistema de pagos.
- Tienda.
- Monedas virtuales.
-----
### Aplicación móvil
No se desarrollarán:

- Android.
- iOS.
-----
### Backend independiente
No se desarrollará un backend separado con:

- Spring Boot.
- Node.js.
- Express.

La aplicación Java trabajará directamente con MongoDB y OpenF1.

-----
## Datos oficiales en tiempo real como requisito
OpenF1 será una **fuente de datos**, pero la simulación no dependerá completamente de que exista una sesión en vivo.

Esto es importante porque nuestro motor de simulación debe poder funcionar con los datos almacenados/configurados.
## Arquitectura
~~~
src/main/java
│
└── com.f1simulator
    │
    ├── model
    │   ├── Driver
    │   ├── Team
    │   ├── Vehicle
    │   ├── Circuit
    │   ├── Simulation
    │   └── Result
    │
    ├── controller
    │   ├── DashboardController
    │   ├── DriverController
    │   ├── VehicleController
    │   ├── CircuitController
    │   └── SimulationController
    │
    ├── service
    │   ├── DriverService
    │   ├── VehicleService
    │   ├── CircuitService
    │   ├── SimulationService
    │   └── OpenF1Service
    │
    ├── repository
    │   ├── DriverRepository
    │   ├── VehicleRepository
    │   ├── CircuitRepository
    │   └── ResultRepository
    │
    ├── simulation
    │   ├── SimulationEngine
    │   ├── LapCalculator
    │   └── PerformanceCalculator
    │
    ├── database
    │   └── MongoConnection
    │
    └── util
~~~
## Y JavaFX:
~~~
src/main/resources
│
├── views
│   ├── dashboard.fxml
│   ├── drivers.fxml
│   ├── vehicles.fxml
│   ├── circuits.fxml
│   └── simulation.fxml
│
└── css
    └── style.css
~~~
## MongoDB
Guardará:
~~~
drivers
teams
vehicles
circuits
simulations
configurations
results
~~~
## OpenF1
Lo utilizaremos principalmente para:
~~~
Pilotos reales
Equipos
Sesiones
Circuitos
Tiempos
Telemetría
Clima
Pit stops
Stints
Resultados históricos
~~~

OpenF1 está precisamente orientado a datos históricos y telemetría de F1.

-----
# 🚀 FUNCIONALIDADES DIFERENCIALES — MOTOR AVANZADO
Estas funcionalidades representan el principal diferencial técnico del proyecto frente a un simulador convencional.

El sistema no se limitará a calcular un tiempo de vuelta estático, sino que utilizará un **motor de simulación dinámico y probabilístico**, capaz de modificar el comportamiento del vehículo durante la sesión según las condiciones ambientales, el desgaste, los eventos y las características del piloto.

Las cuatro funcionalidades principales serán:

1. Motor probabilístico de simulación.
1. Clima dinámico.
1. Sistema de eventos aleatorios.
1. Telemetría visual en tiempo real.
-----
## ÉPICA E06 — Motor probabilístico de simulación
El sistema deberá generar resultados dinámicos y no completamente deterministas.

Una misma configuración podrá producir diferentes resultados en distintas simulaciones debido a factores como:

- Variabilidad del piloto.
- Variabilidad del vehículo.
- Condiciones de pista.
- Clima.
- Desgaste.
- Combustible.
- Estrategia.
- Eventos.
- Variación aleatoria controlada.
## HU-28 — Simulación probabilística
**Como sistema**, quiero introducir variabilidad controlada en los cálculos de rendimiento, para generar simulaciones realistas y evitar que una misma configuración produzca siempre exactamente el mismo resultado.
### Modelo conceptual
~~~ text
CARACTERÍSTICAS DEL PILOTO
            +
CARACTERÍSTICAS DEL VEHÍCULO
            +
CIRCUITO
            +
CLIMA
            +
NEUMÁTICOS
            +
COMBUSTIBLE
            +
DESGASTE
            +
ESTRATEGIA
            +
EVENTOS
            +
VARIABILIDAD
            ↓
     MOTOR PROBABILÍSTICO
            ↓
       TIEMPO DE VUELTA
~~~

La aleatoriedad deberá estar controlada mediante rangos y modificadores, evitando resultados completamente arbitrarios.
### Ejemplo
Una misma configuración podría producir:
~~~ text
Simulación 1 → 1:10.231
Simulación 2 → 1:10.184
Simulación 3 → 1:10.297
Simulación 4 → 1:10.156
Simulación 5 → 1:10.263
~~~

La diferencia deberá mantenerse dentro de un rango razonable.

-----
## ÉPICA E07 — Clima dinámico
El clima no será una condición estática de la sesión.

Las condiciones meteorológicas podrán cambiar durante la clasificación y afectar progresivamente el rendimiento.
## HU-29 — Evolución climática
**Como sistema**, quiero modificar las condiciones climáticas durante la simulación, para representar una pista cuyo estado cambia durante la sesión.
### Variables climáticas
~~~ text
Temperatura
Humedad
Probabilidad de lluvia
Intensidad de lluvia
Temperatura de pista
Estado de pista
~~~
### Estados posibles
~~~ text
☀️ SECO
🌥️ NUBLADO
🌦️ LLUVIA LIGERA
🌧️ LLUVIA
⛈️ LLUVIA INTENSA
~~~
### Ejemplo
~~~ text
Q1
☀️ 24°C
Pista seca
Grip: 82%

        ↓

Q2
🌥️ 22°C
40% probabilidad de lluvia
Grip: 87%

        ↓

Q3
🌧️ 18°C
Pista húmeda
Grip: 73%
~~~

El cambio climático deberá afectar:

- Tiempo de vuelta.
- Tracción.
- Frenado.
- Temperatura de neumáticos.
- Desgaste.
- Estrategia.
- Rendimiento del piloto.
- Selección de neumáticos.
-----
## ÉPICA E08 — Sistema de eventos aleatorios
Durante una simulación podrán ocurrir eventos que modifiquen temporalmente el rendimiento.
## HU-30 — Generar eventos
**Como sistema**, quiero generar eventos durante la simulación, para introducir situaciones inesperadas que afecten el resultado de una sesión.
### Eventos posibles
#### 🟢 Eventos positivos
~~~ text
Track Evolution
Mejora del grip de la pista.

Perfect Lap
El piloto consigue una vuelta excepcional.

Clean Air
El piloto encuentra una vuelta sin tráfico.
~~~
#### 🟡 Eventos neutros
~~~ text
Traffic
Tráfico durante una vuelta.

Cambio climático
Modificación de las condiciones.

Track Evolution
La pista continúa mejorando.
~~~
#### 🔴 Eventos negativos
~~~ text
Driver Mistake
Error del piloto.

Lock Up
Bloqueo de neumáticos.

Yellow Flag
Reducción de velocidad en un sector.

Tyre Overheating
Sobrecalentamiento de neumáticos.

Heavy Traffic
Pérdida de tiempo por tráfico.
~~~
### Ejemplo
~~~ text
╔══════════════════════════════════╗
║         LIVE EVENT               ║
╠══════════════════════════════════╣
║ ⚠️ YELLOW FLAG                   ║
║                                  ║
║ Sector 2                         ║
║                                  ║
║ +0.421s                          ║
╚══════════════════════════════════╝
~~~

Los eventos deberán modificar variables del motor de simulación y no limitarse a ser mensajes visuales.

-----
## ÉPICA E09 — Telemetría visual
La aplicación deberá mostrar información de la simulación en tiempo real.
## HU-31 — Visualizar telemetría
**Como usuario**, quiero visualizar los principales parámetros del vehículo durante la simulación, para analizar su comportamiento en tiempo real.
### Datos mostrados
~~~ text
Velocidad
RPM
Combustible
Desgaste de neumáticos
Temperatura de neumáticos
Temperatura del motor
Sector actual
Tiempo de vuelta
Delta
Estado de pista
~~~
### Dashboard conceptual
~~~ text
╔════════════════════════════════════════════╗
║              LIVE TELEMETRY                ║
╠════════════════════════════════════════════╣
║ SPEED       312 km/h                       ║
║ RPM         ███████████████░  11,842       ║
║ FUEL        ███████░░░░░░░░  48%           ║
║ TYRE WEAR   █████████░░░░░░  72%           ║
║ ENGINE      ███████████░░░░  91°C          ║
║ TYRES       █████████░░░░░  104°C          ║
╠════════════════════════════════════════════╣
║ SECTOR 1       22.341s                     ║
║ SECTOR 2       24.512s                     ║
║ SECTOR 3       23.381s                     ║
║                                               
║ LAP            1:10.234                    ║
║ DELTA          -0.183s 🟢                  ║
╚════════════════════════════════════════════╝
~~~

-----
# HU-32 — Visualizar evolución de la vuelta
**Como usuario**, quiero visualizar la evolución del tiempo y rendimiento durante cada vuelta, para identificar dónde se gana o pierde tiempo.

Se utilizarán elementos gráficos de JavaFX como:
~~~ text
LineChart
BarChart
ProgressBar
TableView
~~~

El sistema podrá mostrar:
~~~ text
Velocidad por vuelta
Tiempo por vuelta
Desgaste
Combustible
Temperatura
Delta
~~~

-----
# HU-33 — Comparar sectores
**Como usuario**, quiero comparar los tiempos de los sectores entre diferentes pilotos, para identificar dónde cada piloto obtiene ventaja.

Ejemplo:
~~~ text
                 VER        LEC

Sector 1       22.341     22.512
Sector 2       24.512     24.201
Sector 3       23.381     23.699
--------------------------------
TOTAL         1:10.234    1:10.412
~~~

El sistema deberá identificar automáticamente:
~~~ text
🟢 Mejor Sector 1 → Verstappen
🟢 Mejor Sector 2 → Leclerc
🟢 Mejor Sector 3 → Verstappen
~~~

-----
## ÉPICA E10 — Evolución dinámica de pista
Esta funcionalidad complementará el clima dinámico y el motor probabilístico.
## HU-34 — Evolución del grip
**Como sistema**, quiero modificar progresivamente el nivel de adherencia de la pista, para representar la evolución de las condiciones durante la sesión.
### Ejemplo
~~~ text
Inicio
Grip: 82%

       ↓

Vueltas completadas

       ↓

Grip: 86%

       ↓

Más goma en pista

       ↓

Grip: 91%
~~~

Si comienza a llover:
~~~ text
Grip: 91%
   ↓
Grip: 84%
   ↓
Grip: 73%
~~~

La evolución de pista afectará directamente al cálculo del tiempo de vuelta.

-----
## ÉPICA E11 — Sistema de estrategia
Las variables de simulación deberán permitir que el usuario tome decisiones que tengan consecuencias.
## HU-35 — Seleccionar estrategia
**Como usuario**, quiero seleccionar una estrategia de conducción, para modificar el equilibrio entre rendimiento y conservación.
### Estrategias
~~~ text
🔥 ATTACK

Rendimiento: +5%
Desgaste: +15%
Consumo: +10%


⚖️ BALANCED

Rendimiento: normal
Desgaste: normal
Consumo: normal


🧊 CONSERVE

Rendimiento: -4%
Desgaste: -30%
Consumo: -15%
~~~

La estrategia deberá interactuar con:
~~~ text
Piloto
Vehículo
Neumáticos
Combustible
Clima
Desgaste
Eventos
~~~

-----
## ÉPICA E12 — Análisis automático de la sesión
## HU-36 — Generar análisis
**Como sistema**, quiero analizar automáticamente los resultados de una sesión, para explicar los principales factores que determinaron el rendimiento.

Al finalizar la simulación el sistema podrá mostrar:
~~~ text
🏆 POLE POSITION

Piloto: Verstappen
Tiempo: 1:10.234

📊 ANÁLISIS

+ Mejor rendimiento en Sector 1
+ Neumático en ventana óptima
+ Baja degradación
+ Condiciones favorables

⚠️ FACTORES NEGATIVOS

- Pérdida de 0.184s en Sector 2
- Tráfico durante la vuelta 4
- Temperatura elevada de neumáticos
~~~

El análisis se generará mediante reglas del propio motor de simulación.

No será necesario implementar inteligencia artificial externa.

-----
-----
# Historias prioritarias
Esto es **muy importante teniendo solo dos personas**.

No todas las HU tienen la misma prioridad.
### 🔴 MVP — Obligatorio
~~~
HU-01  Equipos
HU-02  Pilotos
HU-03  Vehículos
HU-04  Circuitos


HU-07  Seleccionar circuito
HU-08  Seleccionar piloto/vehículo
HU-09  Conducción
HU-10  Aerodinámica
HU-11  Neumáticos
HU-12  Combustible


HU-14  Clima
HU-15  Cálculo de rendimiento
HU-16  Tiempo de vuelta
HU-17  Ejecutar clasificación
HU-18  Clasificación


HU-20  Resultado
HU-21  Estadísticas
HU-22  Clima


HU-24  OpenF1
HU-25  MongoDB
HU-26  Guardar resultados
~~~
### 🟡 Si alcanza el tiempo
~~~
HU-05  Consulta general
HU-06  Comparación de vehículos
HU-13  Guardar configuración
HU-19  Evolución
HU-27  Historial
~~~
### 🟢 Bonus
~~~
HU-23  Telemetría avanzada
~~~
## Requisitos funcionales — RF
Ahora convertimos las historias en requisitos.
### RF-01
El sistema deberá permitir registrar, editar, consultar y eliminar equipos.
### RF-02
El sistema deberá permitir registrar, editar, consultar y eliminar pilotos.
### RF-03
El sistema deberá permitir registrar, editar, consultar y eliminar vehículos.
### RF-04
El sistema deberá permitir registrar, editar, consultar y eliminar circuitos.
### RF-05
El sistema deberá permitir relacionar pilotos con equipos y vehículos.
### RF-06
El sistema deberá permitir seleccionar circuito, piloto y vehículo para una simulación.
### RF-07
El sistema deberá permitir configurar el modo de conducción.
### RF-08
El sistema deberá permitir configurar la carga aerodinámica.
### RF-09
El sistema deberá permitir configurar la presión de neumáticos.
### RF-10
El sistema deberá permitir configurar la estrategia de combustible.
### RF-11
El sistema deberá obtener o generar las condiciones climáticas de la sesión.
### RF-12
El sistema deberá calcular el rendimiento de los vehículos considerando las características configuradas.
### RF-13
El sistema deberá calcular tiempos de vuelta.
### RF-14
El sistema deberá ordenar los participantes según su tiempo de vuelta.
### RF-15
El sistema deberá mostrar la clasificación final.
### RF-16
El sistema deberá mostrar estadísticas mediante elementos gráficos.
### RF-17
El sistema deberá consumir información de OpenF1.
### RF-18
El sistema deberá almacenar información en MongoDB.
### RF-19
El sistema deberá guardar los resultados de las simulaciones.
### RF-20
El sistema deberá permitir consultar resultados almacenados.
### RF-21
El sistema deberá informar al usuario cuando ocurra un error durante la consulta de OpenF1.

-----
## Requisitos no funcionales — RNF
Aquí sí debemos ser **realistas**. No pondría cosas como "99.99% de disponibilidad" porque es un proyecto académico de tres días.
### RNF-01 — Lenguaje
El sistema deberá desarrollarse utilizando **Java 17**.
### RNF-02 — Interfaz
La interfaz deberá desarrollarse utilizando **JavaFX** y imagenes SVJ.
### RNF-03 — Persistencia
La información persistente deberá almacenarse utilizando **MongoDB**.
### RNF-04 — Integración
El sistema deberá consumir la API de **OpenF1** mediante solicitudes HTTP.
### RNF-05 — Arquitectura
El sistema deberá mantener separación entre:
~~~
UI
Controllers
Services
Models
Repositories
API
Simulation
~~~

Esto es importante para que no terminen con toda la lógica dentro de los Controllers.
### RNF-06 — Mantenibilidad
El código deberá organizarse mediante paquetes y responsabilidades claramente definidas.
### RNF-07 — Validación
El sistema deberá validar los datos ingresados antes de almacenarlos.

Por ejemplo:
~~~
Velocidad > 0
Longitud > 0
Vueltas > 0
Nombre ≠ vacío
~~~
### RNF-08 — Manejo de errores
El sistema deberá mostrar mensajes comprensibles cuando ocurra un error de entrada, persistencia o comunicación con OpenF1.
### RNF-09 — Rendimiento
Las operaciones normales de consulta y navegación deberán ejecutarse sin bloqueos perceptibles en la interfaz.
### RNF-10 — Usabilidad
La interfaz deberá permitir que el usuario pueda iniciar una simulación sin necesidad de interactuar directamente con MongoDB o la API.
### RNF-11 — Portabilidad
La aplicación deberá poder ejecutarse en un entorno compatible con Java 17 y las dependencias definidas en Maven.
### RNF-12 — Control de versiones
El código deberá gestionarse mediante Git y GitHub, utilizando commits descriptivos y convenciones establecidas por el equipo.

-----
# ESTÁNDARES DE IMPLEMENTACIÓN — POO Y CALIDAD DE CÓDIGO
El proyecto deberá desarrollarse aplicando principios de **Programación Orientada a Objetos (POO)** y buenas prácticas de desarrollo.

El objetivo no será únicamente obtener una aplicación funcional, sino demostrar una implementación estructurada, mantenible, reutilizable y correctamente organizada.

-----
# Prioridad de implementación
Las funcionalidades y características técnicas se dividirán en tres niveles.
## 🔴 PRIORIDAD MÁXIMA — CORE DEL PROYECTO
Estas características son indispensables para que el proyecto sea considerado completo.
### Funcionalidades
~~~ text
Motor probabilístico
Clima dinámico
Eventos aleatorios
Telemetría visual
Motor de simulación
Clasificación Q1 → Q2 → Q3
~~~
### Arquitectura y POO
~~~ text
Separación de responsabilidades
Encapsulamiento
Herencia
Polimorfismo
Clases y objetos
Interfaces
Patrones de diseño
Manejo correcto de excepciones
Concurrencia mediante hilos cuando sea necesario
~~~

-----
# 🟠 PRIORIDAD ALTA — CALIDAD DEL SISTEMA
~~~ text
Lambda
Streams
Sobrecarga
Getters y Setters
Excepciones personalizadas
Utils reutilizables
Validaciones
Comentarios técnicos
Organización de packages
~~~

Estas características deberán implementarse como parte natural del sistema y no únicamente para cumplir un requisito.

-----
# 🟡 PRIORIDAD MEDIA — EXTRAS
~~~ text
Análisis automático
Comparación de sectores
Evolución avanzada de pista
Historial avanzado
Comparación entre simulaciones
Predicciones
~~~

Los extras podrán implementarse únicamente después de garantizar el correcto funcionamiento del núcleo de simulación y de la arquitectura POO.

-----
# REQUISITOS NO FUNCIONALES — POO Y CALIDAD DE CÓDIGO
## RNF-18 — Programación Orientada a Objetos
El sistema deberá desarrollarse aplicando los principios fundamentales de la Programación Orientada a Objetos:

- Encapsulamiento.
- Abstracción.
- Herencia.
- Polimorfismo.

Las clases deberán representar responsabilidades concretas dentro del dominio del sistema.

-----
## RNF-19 — Encapsulamiento
Los atributos de las clases deberán mantenerse privados cuando corresponda.

El acceso y modificación de los datos deberá realizarse mediante métodos públicos controlados, principalmente:
~~~ text
getters
setters
métodos de comportamiento
~~~

No se permitirá exponer innecesariamente los atributos internos de los objetos.

-----
## RNF-20 — Uso de `this`
La palabra reservada `this` deberá utilizarse cuando sea necesario para diferenciar atributos de parámetros, acceder explícitamente a miembros del objeto o mejorar la claridad del código.

Su utilización deberá responder a una necesidad real y no ser agregada artificialmente.

-----
## RNF-21 — Clases y objetos
El sistema deberá modelar las entidades principales mediante clases y objetos.

Entre ellas:
~~~ text
Driver
Team
Vehicle
Circuit
Simulation
Result
Weather
Telemetry
Event
Strategy
~~~

Cada clase deberá poseer una responsabilidad definida.

-----
## RNF-22 — Visibilidad de clases
Las clases deberán utilizar correctamente los modificadores de acceso:
~~~ text
public
private
protected
~~~

Las clases y miembros deberán tener la visibilidad mínima necesaria para cumplir su responsabilidad.

No se deberán utilizar miembros `public` cuando puedan mantenerse encapsulados.

-----
## RNF-23 — Herencia
Se deberá utilizar herencia únicamente cuando exista una relación lógica de especialización entre clases.

Ejemplo conceptual:
~~~ text
Event
 │
 ├── WeatherEvent
 ├── DriverEvent
 └── TrackEvent
~~~

La herencia no deberá utilizarse únicamente para aumentar artificialmente la cantidad de clases.

-----
## RNF-24 — Polimorfismo
El sistema deberá aprovechar el polimorfismo para permitir que diferentes implementaciones puedan ser utilizadas mediante una misma abstracción.

Ejemplo conceptual:
~~~ text
DrivingStrategy
      │
      ├── AggressiveStrategy
      ├── BalancedStrategy
      └── ConservativeStrategy
~~~

El motor de simulación deberá trabajar con la abstracción `DrivingStrategy` sin depender directamente de una implementación específica.

Esto permitirá modificar las estrategias sin modificar el núcleo de simulación.

-----
## RNF-25 — Sobrecarga
Se podrá utilizar sobrecarga de métodos y constructores cuando existan diferentes formas válidas de realizar una misma operación.

Ejemplos:
~~~ text
Constructores con diferentes parámetros
Métodos de configuración con diferentes argumentos
Métodos de búsqueda con diferentes criterios
~~~

La sobrecarga deberá utilizarse únicamente cuando mejore la reutilización y legibilidad del código.

-----
## RNF-26 — Interfaces y abstracciones
Las funcionalidades que puedan tener múltiples implementaciones deberán poder representarse mediante interfaces o clases abstractas.

Ejemplos:
~~~ text
DrivingStrategy
WeatherStrategy
Event
F1DataProvider
~~~

Esto permitirá aplicar polimorfismo y reducir el acoplamiento entre componentes.

-----
# LAMBDA Y STREAMS
## RNF-27 — Expresiones Lambda
El sistema deberá utilizar expresiones Lambda cuando permitan simplificar operaciones funcionales sin reducir la legibilidad.

Se podrán utilizar para:
~~~ text
Filtrado
Ordenamiento
Recorridos
Eventos
Callbacks de JavaFX
Operaciones sobre colecciones
~~~

No deberán utilizarse cuando una implementación tradicional sea considerablemente más clara.

-----
## RNF-28 — Streams
Los Streams de Java podrán utilizarse para operaciones sobre colecciones como:
~~~ text
Filtrar pilotos
Ordenar clasificación
Buscar vehículos
Obtener mejores tiempos
Calcular estadísticas
Agrupar resultados
~~~

El uso de Streams deberá mantenerse legible y no deberá generar cadenas excesivamente complejas.

-----
# CONCURRENCIA Y HILOS
## RNF-29 — Hilos
Las operaciones que puedan tardar o ejecutarse continuamente deberán utilizar mecanismos de concurrencia cuando sea necesario.

Especialmente:
~~~ text
Simulación
Telemetría
Consumo de OpenF1
Actualizaciones periódicas
Procesamiento de datos
~~~

El hilo principal de JavaFX no deberá bloquearse durante operaciones prolongadas.

-----
## RNF-30 — JavaFX Application Thread
Las actualizaciones de componentes visuales deberán realizarse de manera segura dentro del contexto de JavaFX.

La lógica pesada del motor de simulación no deberá ejecutarse directamente sobre el hilo de interfaz cuando pueda provocar congelamiento de la aplicación.
### Flujo conceptual
~~~ text
JavaFX UI
   │
   ▼
Usuario inicia simulación
   │
   ▼
Hilo de simulación
   │
   ├── Calcula vuelta
   ├── Procesa clima
   ├── Procesa eventos
   ├── Actualiza telemetría
   │
   ▼
Actualización segura de JavaFX
   │
   ▼
Dashboard
~~~

-----
# MANEJO DE EXCEPCIONES
## RNF-31 — Excepciones
El sistema deberá utilizar excepciones para representar errores que puedan ocurrir durante la ejecución.

Se deberán contemplar situaciones como:
~~~ text
Datos inválidos
Error de conexión con MongoDB
Error de comunicación con OpenF1
Datos inexistentes
Configuración inválida
Error durante la simulación
~~~

-----
## RNF-32 — Try/Catch
Las estructuras `try/catch` deberán utilizarse únicamente alrededor de operaciones que puedan generar excepciones.

No se deberá utilizar:
~~~ text
try/catch
~~~

como mecanismo general para ocultar errores.

Los errores deberán ser manejados de forma específica y proporcionar información útil al usuario o al sistema.

-----
## RNF-33 — Excepciones personalizadas
Cuando sea necesario, se podrán crear excepciones específicas del dominio.

Ejemplos:
~~~ text
InvalidSimulationException
InvalidVehicleConfigurationException
OpenF1ConnectionException
DatabaseException
InvalidDriverException
~~~

Esto permitirá diferenciar errores de negocio de errores técnicos.

-----
# SYSTEM.EXIT
## RNF-34 — Terminación de la aplicación
`System.exit(0)` deberá utilizarse únicamente cuando sea necesario finalizar completamente la aplicación.

No deberá utilizarse como mecanismo habitual para controlar el flujo de la aplicación ni para solucionar errores.

La aplicación deberá priorizar mecanismos adecuados de cierre de JavaFX y liberación de recursos.

-----
# PACKAGES
## RNF-35 — Organización mediante Packages
El proyecto deberá organizarse mediante packages de acuerdo con las responsabilidades de cada componente.

La estructura deberá mantener una separación similar a:
~~~ text
com.f1simulator
│
├── model
│
├── controller
│
├── service
│
├── repository
│
├── simulation
│
├── database
│
├── api
│
├── strategy
│
├── event
│
├── telemetry
│
└── util
~~~

Cada package deberá contener clases relacionadas con una responsabilidad común.

-----
# IMPORTS Y UTILIDADES
## RNF-36 — Package Utils
Las funcionalidades reutilizables y transversales deberán mantenerse en packages de utilidad.

Ejemplos:
~~~ text
DateUtils
ValidationUtils
FormatUtils
MathUtils
RandomUtils
~~~

Las clases `Utils` no deberán convertirse en contenedores gigantes de métodos sin relación.

Cada utilidad deberá tener una responsabilidad clara.

-----
## RNF-37 — Imports
Los imports deberán mantenerse organizados y únicamente deberán incluir dependencias realmente utilizadas por cada clase.

No deberán existir imports innecesarios.

-----
# CALIDAD DEL CÓDIGO
## RNF-38 — No código espagueti
El proyecto deberá evitar código espagueti.

No se permitirá concentrar en una única clase:
~~~ text
Interfaz
MongoDB
OpenF1
Lógica de negocio
Simulación
Validaciones
Cálculos
Eventos
Telemetría
~~~

Las responsabilidades deberán distribuirse entre las capas correspondientes.

-----
## RNF-39 — Single Responsibility
Cada clase deberá tener una responsabilidad principal claramente definida.

Ejemplo:
~~~ text
SimulationEngine
→ Ejecutar la simulación.

PerformanceCalculator
→ Calcular rendimiento.

LapCalculator
→ Calcular tiempos de vuelta.

WeatherService
→ Gestionar condiciones climáticas.

TelemetryService
→ Procesar telemetría.

EventManager
→ Gestionar eventos.

SimulationRepository
→ Persistir resultados.
~~~

-----
## RNF-40 — Comentarios
El código deberá incluir comentarios cuando sea necesario explicar:

- Decisiones de diseño.
- Fórmulas complejas.
- Comportamientos no evidentes.
- Reglas de simulación.
- Algoritmos.
- Integraciones externas.

Los comentarios deberán ser **detallados pero no invasivos**.

No se deberán utilizar comentarios para explicar operaciones obvias.

El código deberá ser suficientemente claro para que los comentarios complementen la implementación en lugar de reemplazarla.

-----
## RNF-41 — Métodos
Los métodos deberán mantenerse enfocados en una responsabilidad concreta.

Se deberán evitar métodos excesivamente largos que realicen múltiples operaciones independientes.

-----
## RNF-42 — Objetos
Los objetos deberán encapsular comportamiento además de datos cuando corresponda.

No se deberá construir un sistema basado únicamente en clases que funcionen como contenedores de atributos.

Las entidades deberán tener métodos relacionados con su propio comportamiento cuando tenga sentido dentro del dominio.

-----
## RNF-43 — Reutilización
La lógica que se utilice en múltiples partes del sistema deberá abstraerse para evitar duplicación.

Se deberá priorizar:
~~~ text
Herencia cuando corresponda
Composición
Interfaces
Polimorfismo
Utils
Services
Patrones de diseño
~~~

antes que copiar y pegar código.

-----
# INTEGRACIÓN CON LOS PATRONES DE DISEÑO
Los requisitos de POO deberán integrarse con los patrones definidos para el proyecto.
~~~ text
STRATEGY
→ Conducción
→ Neumáticos
→ Estrategias de simulación

OBSERVER
→ Telemetría
→ Dashboard
→ Eventos visuales

FACTORY
→ Creación de estrategias
→ Creación de eventos

ADAPTER
→ OpenF1

REPOSITORY
→ MongoDB

FACADE
→ Inicio y coordinación de simulaciones
~~~

El uso de patrones deberá responder a necesidades reales del sistema.

No se deberán agregar patrones únicamente para aumentar la cantidad de patrones declarados en la documentación.

-----
# PRINCIPIOS GENERALES DE DESARROLLO
El equipo deberá priorizar:
~~~ text
✔ POO real
✔ Encapsulamiento
✔ Abstracción
✔ Herencia
✔ Polimorfismo
✔ Composición
✔ Bajo acoplamiento
✔ Alta cohesión
✔ Separación de responsabilidades
✔ Reutilización
✔ Excepciones correctamente manejadas
✔ Concurrencia controlada
✔ Código legible
✔ Packages organizados
✔ Comentarios útiles
✔ No código espagueti
~~~

El objetivo final es que la aplicación no solamente funcione, sino que pueda ser explicada técnicamente y demostrar una correcta aplicación de los principios de ingeniería de software.

-----
-----
# MOTOR DE SIMULACIÓN AVANZADO
Con estas funcionalidades, el motor evolucionará de un simple cálculo de tiempo a un sistema compuesto por múltiples factores.
~~~ text
                         PILOTO
                            │
                         VEHÍCULO
                            │
                         CIRCUITO
                            │
                        ESTRATEGIA
                            │
                        NEUMÁTICOS
                            │
                        COMBUSTIBLE
                            │
                           CLIMA
                            │
                     ESTADO DE PISTA
                            │
                         DESGASTE
                            │
                         EVENTOS
                            │
                      ALEATORIEDAD
                            ▼
                 ┌────────────────────┐
                 │  SIMULATION ENGINE │
                 └─────────┬──────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
          SECTORES     TELEMETRÍA    EVENTOS
              │            │            │
              └────────────┼────────────┘
                           ▼
                    TIEMPO DE VUELTA
                           │
                           ▼
                     CLASIFICACIÓN
                           │
                           ▼
                    ANÁLISIS FINAL
~~~

-----
# PATRONES DE DISEÑO UTILIZADOS EN EL MOTOR
## Las nuevas funcionalidades deberán aprovechar los patrones definidos en la arquitectura.
## Strategy
Se utilizará para:
~~~ text
Estrategias de conducción
Estrategias de neumáticos
Estrategias de rendimiento
~~~
## Observer
Se utilizará para:
~~~ text
Telemetría
Actualización del dashboard
Eventos de simulación
Gráficas
~~~
## Factory
Se utilizará para crear:
~~~ text
Estrategias
Eventos
Configuraciones
~~~
## Adapter
Se utilizará para desacoplar:
~~~ text
OpenF1 API
~~~

del resto del sistema.
## Facade
Se utilizará para simplificar:
~~~ text
Inicio de simulación
Ejecución de Q1/Q2/Q3
Obtención de datos
Almacenamiento de resultados
~~~

-----
## NUEVA PRIORIDAD DEL PROYECTO
Estas funcionalidades pasan a formar parte del núcleo diferenciador.
### 🔴 DIFERENCIAL — PRIORIDAD MÁXIMA
~~~ text
HU-28  Motor probabilístico
HU-29  Clima dinámico
HU-30  Eventos aleatorios
HU-31  Telemetría visual
~~~
### 🟠 PRIORIDAD ALTA
~~~ text
HU-32  Evolución de vueltas
HU-33  Comparación de sectores
HU-34  Evolución de pista
HU-35  Sistema de estrategia
HU-36  Análisis automático
~~~

Estas funcionalidades deberán integrarse con el motor de simulación existente y no funcionar como módulos aislados.

El objetivo es que cada variable tenga consecuencias sobre el resultado final.
~~~ text
CAMBIAR CLIMA
      ↓
CAMBIA GRIP
      ↓
CAMBIA NEUMÁTICOS
      ↓
CAMBIA DESGASTE
      ↓
CAMBIA RENDIMIENTO
      ↓
CAMBIA TIEMPO
      ↓
CAMBIA CLASIFICACIÓN
~~~

De esta manera, el resultado final de una simulación será consecuencia de la interacción de múltiples variables y no de un único cálculo estático.
