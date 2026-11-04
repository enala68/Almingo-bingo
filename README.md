# 🎲 Almingo Bingo & Sustainability Challenge (Java Desktop Application)

![Java](https://img.shields.io/badge/Java-JDK%2017%2B-red)
![Database](https://img.shields.io/badge/Database-MySQL%20%2F%20PostgreSQL-blue)
![Focus](https://img.shields.io/badge/Focus-IT%20Sustainability-green)

Aplicación multijugador de escritorio para la gamificación de la Sostenibilidad mediante un juego de Bingo Colectivo, desarrollada en Java con integración a Base de Datos relacional[cite: 1, 6, 7].

---

## 📌 Descripción del Proyecto

Este software combina la dinámica tradicional de un Bingo colectivo con preguntas y respuestas interactivas sobre **Sostenibilidad aplicada al Sector Informático (ASG)**[cite: 3, 6]. Para cantar **Línea** o **Bingo**, los jugadores deberán validar sus conocimientos respondiendo correctamente a preguntas seleccionadas sin repetición[cite: 2].

---

## ✨ Características Principales

* 🎮 **Entorno Multijugador:** Control dinámico de acciones y estado de partida para múltiples jugadores simultáneos en sala[cite: 1, 6, 7].
* 🌱 **Validación por Sostenibilidad:** Sistema de comprobación de premios con al menos 20 preguntas/respuestas temáticas (IT & ASG) sin repetición durante la partida[cite: 2, 3].
* 🗄️ **Persistencia en BBDD:** Base de datos en 3FN para la gestión de usuarios, partidas, preguntas y registros históricos[cite: 1].
* ⚡ **Comprobación Automática:** Automatización de la verificación de cartones y líneas premiadas[cite: 1].

---

## 🛠️ Arquitectura y Tecnologías

* **Lenguaje de Programación:** Java (Programación Orientada a Objetos, Colecciones, Estructuras de Control y Manejo de Ficheros/Persistencia)[cite: 1, 5].
* **Base de Datos:** Modelo relacional normalizado en 3FN (MySQL / PostgreSQL / MariaDB)[cite: 1, 5].
* **Control de Versiones:** Git con estrategia de ramas para el trabajo colaborativo del equipo[cite: 2].

---

## 🗄️ Modelo de Base de Datos

La base de datos sigue un diseño de 3ª Forma Normal (3FN), garantizando la ausencia de redundancias e integrando la gestión de[cite: 1]:
1. **Usuarios / Jugadores:** Registro e historial[cite: 1].
2. **Banco de Preguntas:** Consultas optimizadas con filtros de sostenibilidad informáticas.
3. **Histórico de Partidas y Resultados:** Inserciones y consultas analíticas avanzadas[cite: 1].

---

## 📂 Estructura del Proyecto

```text
src/
├── controller/       # Lógica del juego y control de flujo
├── model/            # Clases POJO, Entidades y Data Access Objects (DAO)
├── view/             # Interfaz de usuario (UI / Consola o GUI)
├── db/               # Scripts SQL (Tablas 3FN, Triggers, Consultas)
└── utils/            # Gestor de preguntas aleatorias y validaciones
