# CamelRace — Sistema de Carreras Multicast en Red Local

CamelRace es un sistema cliente–servidor que organiza carreras de camellos en una red local.  
Los clientes se conectan al servidor vía TCP, reciben la asignación de su grupo y se unen a una dirección Multicast UDP donde se simula la carrera en tiempo real.

## 1. Descripción General

El servidor agrupa automáticamente a los jugadores en equipos de 3–4 clientes y les asigna:

- Una IP multicast exclusiva
- Un puerto
- Un idGrupo
- Una semilla de la carrera

Los clientes se unen al canal multicast y reciben los eventos de la carrera en tiempo real.

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

* **JavaFX 21.0.6 (funciona con JDK 17)**
* **JDK 17**
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

   
   El cliente ahora acepta parámetros desde consola:
```
--id    identificador del jugador  
--host  dirección del servidor


java -jar CamelRace.jar --id=jugador03 --host=192.168.1.38
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