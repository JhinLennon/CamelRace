# CamelRace

Sistema cliente–servidor que organiza carreras de camellos en red local. El servidor forma grupos de jugadores, asigna direcciones multicast y coordina la ejecución de una carrera distribuida mediante intercambio de objetos serializados.

## 1. Descripción General

CamelRace implementa un modelo **TCP + Multicast UDP** para permitir que varios clientes se descubran, reciban un canal exclusivo y participen en una carrera simulada.
El servidor acepta conexiones, forma grupos de 3–4 clientes y asigna a cada grupo una **IP multicast única** junto con un puerto.
Los clientes reciben esta información, se unen al grupo multicast y envían/reciben objetos que representan eventos de la carrera.

## 2. Estructura del Proyecto

```
src/main/java/
├── cliente/ClienteTCP.java
├── mensajes/
│   ├── AsignacionGrupo.java
│   └── SolicitudConexion.java
├── programa/
│   ├── Cliente.java
│   ├── ClienteMulticastUDP.java
│   ├── DatosCarrera.java
│   └── Jugador.java
├── servidor/Servidor.java
└── tarea/camelrace/
    ├── CamelApplication.java
    ├── CamelController.java
    └── Launcher.java
```

### Componentes principales

* **Servidor**

    * Acepta clientes vía TCP.
    * Agrupa automáticamente en grupos de tamaño fijo (3 o 4).
    * Asigna IPs multicast del rango `231.0.0.1` a `231.0.0.3` rotando.
    * Protege el idGrupo mediante un semáforo.

* **ClienteTCP**

    * Solicita conexión.
    * Recibe objeto `AsignacionGrupo` con datos del grupo.

* **ClienteMulticastUDP**

    * Se une al canal multicast asignado.
    * Envía/recibe objetos del progreso de la carrera.

* **Modelo de Carrera**

    * `DatosCarrera`, `Jugador` y eventos distribuidos.

* **Interfaz (JavaFX)**

    * Control y visualización de la carrera.

## 3. Requisitos

* **JavaFX 21.0.6**
* **JDK 25**
* Proyecto **Maven**
* Todos los clientes deben ejecutarse en la misma LAN.

## 4. Compilación y Ejecución

### Compilación

El proyecto se construye automáticamente desde IntelliJ usando Maven.

### Ejecución

1. **Iniciar el servidor**

   ```
   Ejecutar Servidor.java desde IntelliJ
   ```
2. **Ejecutar cada cliente (hasta 4 por grupo)**

   ```
   Ejecutar CamelApplication.java o Cliente.java
   ```
3. Al completarse un grupo, el servidor asigna:

    * IP multicast (231.0.0.x)
    * Puerto
    * idGrupo
    * Semilla de la carrera

Los clientes se unen al canal multicast y empieza la simulación.

## 5. Protocolo por Objetos

Se envían únicamente objetos serializados:

* `SolicitudConexion`
* `AsignacionGrupo`
* Objetos de la carrera (pasos, eventos, ranking, fin de carrera)

## 6. Problemas Conocidos

* **Windows puede seleccionar una interfaz de red incorrecta**, provocando que algunos clientes no reciban multicast (especialmente en WiFi).
* La conexión **Ethernet elimina este problema**.
* Se recomienda seleccionar explícitamente la interfaz multicast mediante `NetworkInterface`.

---

## 7. Guía de Pruebas

### ✔ Prueba 1 — Un grupo completo (3 o 4 clientes)

**Objetivo:** validar emparejamiento y asignación multicast
**Acción:** iniciar servidor, lanzar clientes hasta completar un grupo
**Resultado esperado:** todos reciben la misma IP multicast y puerto; comienza la carrera

### ✔ Prueba 2 — Dos grupos simultáneos

**Objetivo:** comprobar que el servidor gestiona varios grupos
**Acción:** ejecutar 6–8 clientes
**Resultado:**

* Grupo 1 → `231.0.0.1`
* Grupo 2 → `231.0.0.2`
  Cada grupo debe correr de forma independiente.

### ✔ Prueba 3 — Repetición del ciclo de IPs

**Objetivo:** asegurar rotación correcta
**Acción:** crear 4 grupos
**Resultado:**

* Grupo 3 → `231.0.0.3`
* Grupo 4 → vuelve a `231.0.0.1` sin mezclar jugadores.

### ✔ Prueba 4 — Desconexión durante la carrera

**Objetivo:** ver comportamiento ante fallos
**Acción:** cerrar un cliente a mitad de carrera
**Resultado:** el resto continúa; la vista refleja ausencia o timeout.

### ✔ Prueba 5 — Fin de carrera sincronizado

**Objetivo:** validar envío único de `FinCarrera`
**Acción:** completar carrera
**Resultado:**
Todos los clientes reciben el mismo ranking en el mismo instante.

---

## 8. Elección de Transporte

* **TCP** para control y emparejamiento → Fiabilidad y orden.
* **Multicast UDP** para la carrera → Bajo coste y difusión simultánea a todos los jugadores.
  Esta combinación satisface los requisitos funcionales y de rendimiento de la práctica.

---

## 9. Concurrencia

El servidor gestiona:

* Semáforo para asignación de idGrupo
* Múltiples hilos de clientes
* Grupos paralelos
* Posibles desconexiones

---

Fin del README.