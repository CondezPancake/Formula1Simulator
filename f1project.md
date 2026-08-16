# **Proyecto: Simulación de Fórmula 1 **

## **Introducción**

La Fórmula 1 es una disciplina que combina velocidad, estrategia y tecnología de vanguardia, generando una experiencia emocionante tanto para los espectadores como para los equipos involucrados. Este proyecto tiene como objetivo desarrollar una **simulación interactiva de Fórmula 1** basada en tecnologías web modernas, permitiendo a los usuarios gestionar y personalizar su experiencia de carrera a través de un sistema dinámico de administración de circuitos, pilotos y vehículos.

## **Objetivos del Proyecto**

Este proyecto busca ofrecer una plataforma interactiva donde los usuarios puedan:

- **Administrar circuitos de carrera** agregando nuevas pistas con características específicas, editando sus datos, eliminándolas o buscándolas en un listado.

- **Gestionar pilotos y vehículos** con atributos personalizados como velocidad, aceleración, resistencia al desgaste de neumáticos y estrategias de carrera.

- **Configurar y personalizar la simulación**, ajustando condiciones climáticas, reglajes de los vehículos y estrategias de equipo.

  

## **Tecnologías Utilizadas**

Para desarrollar una simulación eficiente y con una interfaz modular y escalable, se emplearán las siguientes tecnologías:

### **Frontend**

- Java 

### **Gestión de Datos**

- **Map, HasMap:** Persistencia temporal de datos para circuitos, pilotos y vehículos.

### **Funcionalidades CRUD**

- **Circuitos:**
  - Agregar nuevos circuitos con nombre, ubicación, distancia y condiciones de pista.
  - Editar y actualizar datos de los circuitos existentes.
  - Eliminar circuitos del sistema.
  - Buscar circuitos por nombre o ubicación.
- **Pilotos:**
  - Agregar pilotos con nombre, equipo, experiencia y habilidades.
  - Modificar atributos del piloto.
  - Eliminar pilotos.
  - Buscar pilotos en el sistema.
- **Vehículos:**
  - Crear y personalizar autos con velocidad máxima, aceleración y consumo de neumáticos.
  - Editar y optimizar configuraciones de los vehículos.
  - Eliminar vehículos del sistema.
  - Buscar vehículos según características.

# Estructura de Datos

```json
// Lista de pilotos con su equipo y rol
const pilotos = [
  { id: 1, nombre: "Max Verstappen", equipo: "Red Bull Racing", rol: "Líder" },
  { id: 2, nombre: "Sergio Pérez", equipo: "Red Bull Racing", rol: "Escudero" },
  { id: 3, nombre: "Lewis Hamilton", equipo: "Mercedes-AMG Petronas", rol: "Líder" },
  { id: 4, nombre: "George Russell", equipo: "Mercedes-AMG Petronas", rol: "Escudero" },
  { id: 5, nombre: "Charles Leclerc", equipo: "Ferrari", rol: "Líder" },
  { id: 6, nombre: "Carlos Sainz", equipo: "Ferrari", rol: "Escudero" },
  { id: 7, nombre: "Lando Norris", equipo: "McLaren", rol: "Líder" },
  { id: 8, nombre: "Oscar Piastri", equipo: "McLaren", rol: "Escudero" },
  { id: 9, nombre: "Fernando Alonso", equipo: "Aston Martin", rol: "Líder" },
  { id: 10, nombre: "Lance Stroll", equipo: "Aston Martin", rol: "Escudero" },
  { id: 11, nombre: "Esteban Ocon", equipo: "Alpine", rol: "Líder" },
  { id: 12, nombre: "Pierre Gasly", equipo: "Alpine", rol: "Escudero" },
  { id: 13, nombre: "Valtteri Bottas", equipo: "Alfa Romeo", rol: "Líder" },
  { id: 14, nombre: "Zhou Guanyu", equipo: "Alfa Romeo", rol: "Escudero" },
  { id: 15, nombre: "Kevin Magnussen", equipo: "Haas", rol: "Líder" },
  { id: 16, nombre: "Nico Hülkenberg", equipo: "Haas", rol: "Escudero" },
  { id: 17, nombre: "Yuki Tsunoda", equipo: "AlphaTauri", rol: "Líder" },
  { id: 18, nombre: "Daniel Ricciardo", equipo: "AlphaTauri", rol: "Escudero" },
  { id: 19, nombre: "Alexander Albon", equipo: "Williams", rol: "Líder" },
  { id: 20, nombre: "Logan Sargeant", equipo: "Williams", rol: "Escudero" }
]

// Lista de equipos con sus pilotos
const equipos = [
  {
    nombre: "Red Bull Racing",
    pais: "Austria",
    motor: "Honda",
    pilotos: [1, 2], // IDs de pilotos
    imagen: "https://upload.wikimedia.org/wikipedia/commons/b/bb/Red_Bull_Racing_Logo.svg"
  },
  {
    nombre: "Mercedes-AMG Petronas",
    pais: "Alemania",
    motor: "Mercedes",
    pilotos: [3, 4],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/3/32/Mercedes_AMG_Petronas_F1_Team_logo.svg"
  },
  {
    nombre: "Ferrari",
    pais: "Italia",
    motor: "Ferrari",
    pilotos: [5, 6],
    imagen: "https://upload.wikimedia.org/wikipedia/en/d/d4/Scuderia_Ferrari_Logo.svg"
  }
];

// Lista de circuitos con estadísticas y ganadores históricos
const circuitos = [
  {
    nombre: "Circuito de Mónaco",
    pais: "Mónaco",
    longitud_km: 3.34,
    vueltas: 78,
    descripcion: "Uno de los circuitos más prestigiosos y difíciles del calendario, conocido por sus calles angostas y la falta de zonas de adelantamiento.",
    record_vuelta: { tiempo: "1:10.166", piloto: "Lewis Hamilton", año: 2019 },
    ganadores: [
      { temporada: 2021, piloto: 1 },
      { temporada: 2022, piloto: 2 },
      { temporada: 2023, piloto: 1 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/4/4e/Monte_Carlo_Formula_1_track_map.svg"
  },
  {
    nombre: "Silverstone",
    pais: "Reino Unido",
    longitud_km: 5.89,
    vueltas: 52,
    descripcion: "Uno de los circuitos más rápidos del calendario, con curvas de alta velocidad como Maggotts y Becketts.",
    record_vuelta: { tiempo: "1:27.097", piloto: "Max Verstappen", año: 2020 },
    ganadores: [
      { temporada: 2021, piloto: 3 },
      { temporada: 2022, piloto: 5 },
      { temporada: 2023, piloto: 1 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/5/5e/Silverstone_Circuit_2020_layout.png"
  },
  {
    nombre: "Circuito de Spa-Francorchamps",
    pais: "Bélgica",
    longitud_km: 7.00,
    vueltas: 44,
    descripcion: "Famoso por la curva Eau Rouge y la larga recta de Kemmel, un circuito donde la potencia del motor es clave.",
    record_vuelta: { tiempo: "1:46.286", piloto: "Valtteri Bottas", año: 2018 },
    ganadores: [
      { temporada: 2021, piloto: 1 },
      { temporada: 2022, piloto: 1 },
      { temporada: 2023, piloto: 1 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/1/1e/Circuit_Spa_2018.png"
  },
  {
    nombre: "Circuito de Monza",
    pais: "Italia",
    longitud_km: 5.79,
    vueltas: 53,
    descripcion: "Conocido como 'El Templo de la Velocidad', Monza es el circuito más rápido del calendario con largas rectas y chicanes icónicas.",
    record_vuelta: { tiempo: "1:21.046", piloto: "Rubens Barrichello", año: 2004 },
    ganadores: [
      { temporada: 2021, piloto: 2 },
      { temporada: 2022, piloto: 1 },
      { temporada: 2023, piloto: 1 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/3/3e/Monza_track_map.svg"
  },
  {
    nombre: "Interlagos",
    pais: "Brasil",
    longitud_km: 4.31,
    vueltas: 71,
    descripcion: "Interlagos es un circuito legendario con cambios de elevación y un trazado técnico que ha sido sede de algunas de las carreras más emocionantes de la historia.",
    record_vuelta: { tiempo: "1:10.540", piloto: "Valtteri Bottas", año: 2018 },
    ganadores: [
      { temporada: 2021, piloto: 3 },
      { temporada: 2022, piloto: 1 },
      { temporada: 2023, piloto: 1 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/2/23/Aut%C3%B3dromo_Jos%C3%A9_Carlos_Pace_%28Interlagos%29.svg"
  },
  {
    nombre: "Circuito de Yas Marina",
    pais: "Emiratos Árabes Unidos",
    longitud_km: 5.28,
    vueltas: 58,
    descripcion: "Ubicado en Abu Dhabi, es famoso por ser el circuito donde se definen muchos campeonatos, con un diseño moderno y una espectacular carrera nocturna.",
    record_vuelta: { tiempo: "1:39.283", piloto: "Lewis Hamilton", año: 2019 },
    ganadores: [
      { temporada: 2021, piloto: 1 },
      { temporada: 2022, piloto: 1 },
      { temporada: 2023, piloto: 3 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/0/0a/Yas_Marina_Circuit_2021_layout.svg"
  },
  {
    nombre: "Circuito de Suzuka",
    pais: "Japón",
    longitud_km: 5.81,
    vueltas: 53,
    descripcion: "Un circuito desafiante con un diseño en forma de ocho, famoso por sus curvas de alta velocidad como 130R y la 'S' de Senna.",
    record_vuelta: { tiempo: "1:30.983", piloto: "Lewis Hamilton", año: 2019 },
    ganadores: [
      { temporada: 2021, piloto: 1 },
      { temporada: 2022, piloto: 1 },
      { temporada: 2023, piloto: 1 }
    ],
    imagen: "https://upload.wikimedia.org/wikipedia/commons/e/eb/Suzuka_circuit_map--2005.svg"
  }
];

// Lista de vehículos con rendimiento detallado
const vehiculos = [
  {
    equipo: "Red Bull Racing",
    modelo: "RB20",
    motor: "Honda",
    velocidad_maxima_kmh: 360,
    aceleracion_0_100: 2.5,
    pilotos: [1, 2], // Max Verstappen y Sergio Pérez
    rendimiento: {
      conduccion_normal: {
        velocidad_promedio_kmh: 320,
        consumo_combustible: { seco: 1.9, lluvioso: 2.1, extremo: 2.4 },
        desgaste_neumaticos: { seco: 1.5, lluvioso: 0.8, extremo: 2.5 }
      },
      conduccion_agresiva: {
        velocidad_promedio_kmh: 340,
        consumo_combustible: { seco: 2.4, lluvioso: 2.6, extremo: 3.0 },
        desgaste_neumaticos: { seco: 2.2, lluvioso: 1.2, extremo: 3.5 }
      },
      ahorro_combustible: {
        velocidad_promedio_kmh: 300,
        consumo_combustible: { seco: 1.6, lluvioso: 1.8, extremo: 2.1 },
        desgaste_neumaticos: { seco: 1.0, lluvioso: 0.5, extremo: 1.8 }
      }
    },
    imagen: "https://upload.wikimedia.org/wikipedia/commons/8/89/Max_Verstappen_2023_Austria_FP2_%28cropped%29.jpg"
  },
  {
    equipo: "Mercedes-AMG Petronas",
    modelo: "W15",
    motor: "Mercedes",
    velocidad_maxima_kmh: 355,
    aceleracion_0_100: 2.6,
    pilotos: [3, 4], // Lewis Hamilton y George Russell
    rendimiento: {
      conduccion_normal: {
        velocidad_promedio_kmh: 315,
        consumo_combustible: { seco: 2.0, lluvioso: 2.2, extremo: 2.5 },
        desgaste_neumaticos: { seco: 1.6, lluvioso: 0.9, extremo: 2.6 }
      },
      conduccion_agresiva: {
        velocidad_promedio_kmh: 335,
        consumo_combustible: { seco: 2.6, lluvioso: 2.8, extremo: 3.2 },
        desgaste_neumaticos: { seco: 2.3, lluvioso: 1.4, extremo: 3.8 }
      },
      ahorro_combustible: {
        velocidad_promedio_kmh: 295,
        consumo_combustible: { seco: 1.7, lluvioso: 1.9, extremo: 2.2 },
        desgaste_neumaticos: { seco: 1.1, lluvioso: 0.6, extremo: 1.9 }
      }
    },
    imagen: "https://upload.wikimedia.org/wikipedia/commons/8/87/Lewis_Hamilton_2022_Imola.jpg"
  }
]
```

# **📌 Historias de Usuario del Proyecto: Simulación de Fórmula 1**

### **🔹 Gestión de Equipos y Pilotos**

1. **Registro de equipos y pilotos**
   - **Como** administrador del sistema
   - **Quiero** poder registrar, editar y eliminar equipos y sus respectivos pilotos
   - **Para** mantener actualizada la base de datos de escuderías y competidores
2. **Consulta de información de pilotos**
   - **Como** usuario
   - **Quiero** ver la lista completa de pilotos junto con su equipo, rol y estadísticas
   - **Para** conocer más sobre los competidores y sus capacidades
3. **Consulta de información de equipos**
   - **Como** usuario
   - **Quiero** ver la lista de equipos con sus respectivos motores y pilotos
   - **Para** identificar qué escuderías están participando en la competencia

------

### **🔹 Gestión de Vehículos**

1. **Registro y gestión de vehículos**
   - **Como** administrador del sistema
   - **Quiero** poder agregar, editar y eliminar vehículos
   - **Para** asegurar que la base de datos de autos esté actualizada con los modelos actuales
2. **Asignación de pilotos a vehículos**
   - **Como** administrador
   - **Quiero** asignar pilotos a un vehículo según su equipo
   - **Para** reflejar correctamente qué autos maneja cada piloto en la competencia
3. **Consulta de información de vehículos**
   - **Como** usuario
   - **Quiero** ver las especificaciones de cada vehículo (velocidad, aceleración, consumo de combustible, desgaste de neumáticos)
   - **Para** analizar su rendimiento y compararlos entre sí
4. **Comparación de vehículos**
   - **Como** usuario
   - **Quiero** comparar dos o más vehículos en cuanto a velocidad, consumo de combustible y desgaste de neumáticos
   - **Para** elegir la mejor estrategia en cada circuito
5. **Selección de vehículo para simulación**
   - **Como** usuario
   - **Quiero** elegir qué vehículo usar en la simulación de clasificación
   - **Para** poder personalizar mi experiencia de juego

------

### **🔹 Gestión de Circuitos**

1. **Registro y gestión de circuitos**

   - **Como** administrador del sistema
   - **Quiero** poder agregar, editar y eliminar circuitos
   - **Para** mantener actualizada la base de datos de pistas disponibles en la simulación

2. **Consulta de estadísticas del circuito**

   - **Como** usuario

   - **Quiero** ver información detallada del circuito (longitud, vueltas, récords de vuelta, ganadores anteriores)

   - **Para** analizar el historial de rendimiento en cada pista

3. **Condiciones climáticas en circuitos**

   - **Como** usuario

   - **Quiero** saber cuál es el clima promedio en cada circuito

   - **Para** anticipar las condiciones de carrera y adaptar mi configuración

4. **Historial de ganadores en circuitos**

   - **Como** usuario

   - **Quiero** ver qué pilotos han ganado en cada circuito en temporadas pasadas

   - **Para** conocer tendencias y el rendimiento de los equipos en cada pista

5. **Impacto del circuito en el rendimiento del vehículo**

   - **Como** usuario

   - **Quiero** saber cómo afecta un circuito al desgaste de neumáticos y consumo de combustible

   - **Para** ajustar mi estrategia de carrera según las características de la pista

6. **Selección de circuito para simulación**

   - **Como** usuario

   - **Quiero** poder elegir un circuito antes de comenzar la simulación

   - **Para** competir en diferentes pistas y evaluar el rendimiento del auto en cada una

------

### **🔹 Configuración del Vehículo**

1. **Configuración de modo de conducción**

   - **Como** usuario

   - **Quiero** elegir entre conducción normal, agresiva o de ahorro de combustible

   - **Para** optimizar mi estrategia durante la clasificación

2. **Ajuste de carga aerodinámica**

   - **Como** usuario

   - **Quiero** modificar la carga aerodinámica de mi vehículo (baja, media, alta)

   - **Para** equilibrar velocidad y estabilidad según el circuito

3. **Ajuste de presión de neumáticos**

   - **Como** usuario

   - **Quiero** ajustar la presión de los neumáticos (baja, estándar, alta)

   - **Para** mejorar la tracción y reducir el desgaste durante la clasificación

4. **Selección de estrategia de combustible**

   - **Como** usuario

   - **Quiero** seleccionar entre estrategia agresiva, balanceada o de ahorro

   - **Para** gestionar el consumo de combustible durante la clasificación

5. **Guardar configuración del vehículo**

   - **Como** usuario

   - **Quiero** que la configuración de mi vehículo se guarde automáticamente

   - **Para** poder reutilizarla en futuras simulaciones sin necesidad de reconfigurar

------

### **🔹 Simulación de Clasificación**

1. **Generación de condiciones climáticas aleatorias**

   - **Como** sistema

   - **Quiero** generar un clima aleatorio para cada sesión de clasificación (seco, lluvioso, extremo)

   - **Para** afectar el rendimiento de los vehículos y hacer la simulación más realista

2. **Cálculo de tiempos de vuelta**

   - **Como** usuario

   - **Quiero** ver el tiempo estimado de vuelta según la configuración del vehículo y las condiciones climáticas

   - **Para** evaluar el desempeño del auto en la clasificación

3. **Clasificación de pilotos en base a tiempos de vuelta**

   - **Como** usuario

   - **Quiero** que los tiempos de vuelta de todos los pilotos sean calculados y ordenados

   - **Para** generar una tabla de posiciones de clasificación

4. **Visualización de la clasificación final**

   - **Como** usuario

   - **Quiero** ver los tiempos de vuelta de todos los pilotos ordenados por posición

   - **Para** conocer qué piloto obtuvo la pole position en la sesión de clasificación

------

### **🔹 Almacenamiento y Datos**

1. **Persistencia de datos de clasificación**

   - **Como** usuario

   - **Quiero** que los resultados de mis sesiones de clasificación se guarden en el sistema

   - **Para** revisar mi desempeño en diferentes circuitos

2. **Historial de configuraciones previas**

   - **Como** usuario

   - **Quiero** ver mis configuraciones de vehículo anteriores

   - **Para** poder compararlas y mejorar mi estrategia en futuras sesiones

3. **Comparación de tiempos de vuelta por circuito**

   - **Como** usuario

   - **Quiero** poder comparar mis tiempos de vuelta en diferentes sesiones de clasificación

   - **Para** analizar mi progreso y ajustar estrategias

