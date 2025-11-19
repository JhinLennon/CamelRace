package tarea.camelrace;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import cliente.ClienteMulticastUDP;
import mensajes.DatosCarrera;
import mensajes.Jugador;
import javafx.animation.AnimationTimer;
import mensajes.AsignacionGrupo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static servidor.Servidor.TAM_GRUPO;

public class CamelController {

    @FXML private Label texto;
    @FXML private Button iniciarCarreraButton;
    @FXML private Button conectarButton;
    @FXML private TextField nombreCamello;
    @FXML private ImageView imagenCamellos1;
    @FXML private ImageView imagenCamellos2;
    @FXML private ImageView imagenCamellos3;
    @FXML private ImageView imagenCamellos4;

    private ImageView[] imagenCamellos;
    private final double META_X = 750;
    private AnimationTimer timer;
    private ClienteMulticastUDP clienteMulticastUDP;
    private String nombrePropioCamello = "";
    private String idJugador = "";
    private boolean carreraTerminada = false;

    private final List<Jugador> jugadoresActuales = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> mapaIndices = new ConcurrentHashMap<>();
    private int siguienteSlot = 0;
    private int jugadoresConectados = 0;
    private AsignacionGrupo asignacionGrupo;

    @FXML
    public void initialize() {
        imagenCamellos = new ImageView[] {
                imagenCamellos1, imagenCamellos2, imagenCamellos3, imagenCamellos4
        };
        iniciarCarreraButton.setDisable(true);
    }

    public void setClienteMulticastUDP(ClienteMulticastUDP cliente) {
        this.clienteMulticastUDP = cliente;
        cliente.setControlador(this);
    }

    public void setAsignacionGrupo(AsignacionGrupo asignacion) {
        this.asignacionGrupo = asignacion;
        this.idJugador = asignacion.getIdJugador();
        this.nombrePropioCamello = asignacion.getIdJugador();
    }

    @FXML
    public void conectar() {
        if (clienteMulticastUDP == null) {
            texto.setText("Error: cliente UDP no configurado.");
            return;
        }

        // Nombre del camello
        if (!nombreCamello.getText().isBlank()) {
            nombrePropioCamello = nombreCamello.getText();
        }

        // Registrar jugador local
        synchronized (jugadoresActuales) {
            boolean ya = jugadoresActuales.stream().anyMatch(j -> j.getId().equals(idJugador));
            if (!ya) {
                Jugador mi = new Jugador(nombrePropioCamello, 0.0, idJugador);
                jugadoresActuales.add(mi);
                asignarIndiceSiHaceFalta(idJugador);
            }
        }

        // Enviar estado inicial
        DatosCarrera datosInicial;
        synchronized (jugadoresActuales) {
            datosInicial = new DatosCarrera(new ArrayList<>(jugadoresActuales), false, null);
        }
        clienteMulticastUDP.enviarDatosCarrera(datosInicial);

        conectarButton.setDisable(true);
        nombreCamello.setDisable(true);

        texto.setText("Conectando... esperando jugadores");

        // Revisar si ya tenemos suficientes jugadores
        Platform.runLater(this::actualizarBotonIniciar);
    }

    @FXML
    protected void onIniciarCarreraClick() {
        if (idJugador == null || idJugador.isBlank()) {
            texto.setText("No hay ID asignado por el servidor.");
            return;
        }

        synchronized (jugadoresActuales) {
            boolean encontrado = jugadoresActuales.stream().anyMatch(j -> j.getId().equals(idJugador));
            if (!encontrado) {
                jugadoresActuales.add(new Jugador(nombrePropioCamello, 0.0, idJugador));
                asignarIndiceSiHaceFalta(idJugador);
            } else {
                jugadoresActuales.stream()
                        .filter(j -> j.getId().equals(idJugador))
                        .findFirst()
                        .ifPresent(j -> j.setNombre(nombrePropioCamello));
            }
        }

        DatosCarrera datosInicial;
        synchronized (jugadoresActuales) {
            datosInicial = new DatosCarrera(new ArrayList<>(jugadoresActuales), false, null);
        }
        clienteMulticastUDP.enviarDatosCarrera(datosInicial);

        if (jugadoresConectados < TAM_GRUPO) {
            texto.setText("Esperando a " + TAM_GRUPO + " jugadores...");
            return;
        }

        texto.setText("¡Corriendo!");
        iniciarCarreraButton.setText("Correr");
        iniciarCarreraButton.setDisable(false);
        carreraTerminada = false;

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                verificarMeta();
            }
        };
        timer.start();

        iniciarCarreraButton.setOnAction(e -> acelerarCamello());
    }

    private void acelerarCamello() {
        if (carreraTerminada) return;
        int idx = obtenerIndicePorId(idJugador);
        if (idx < 0 || idx >= imagenCamellos.length) return;

        double velocidad = Math.random() * 20 + 10;
        double nuevaPosicion = imagenCamellos[idx].getX() + velocidad;
        imagenCamellos[idx].setX(nuevaPosicion);

        actualizarJugadorLocal(idJugador, nuevaPosicion);

        DatosCarrera datos;
        synchronized (jugadoresActuales) {
            datos = new DatosCarrera(new ArrayList<>(jugadoresActuales), true, null);
        }
        clienteMulticastUDP.enviarDatosCarrera(datos);

        if (nuevaPosicion >= META_X) {
            datos.setGanador(nombrePropioCamello);
            clienteMulticastUDP.enviarDatosCarrera(datos);
            carreraTerminada();
        }
    }

    private void verificarMeta() {
        int idx = obtenerIndicePorId(idJugador);
        if (idx >= 0 && imagenCamellos[idx].getX() >= META_X) {
            carreraTerminada = true;
            if (timer != null) timer.stop();
            carreraTerminada();
        }
    }

    private void carreraTerminada() {
        iniciarCarreraButton.setDisable(true);
        texto.setText("¡Meta alcanzada!");
        carreraTerminada = true;
        if (timer != null) timer.stop();
    }


    public void actualizarDatosJugadores(DatosCarrera datos) {
        if (datos == null || datos.getJugadores() == null) return;

        synchronized (jugadoresActuales) {
            for (Jugador nuevoJugador : datos.getJugadores()) {
                asignarIndiceSiHaceFalta(nuevoJugador.getId());

                boolean encontrado = false;
                for (Jugador j : jugadoresActuales) {
                    if (j.getId().equals(nuevoJugador.getId())) {
                        j.setNombre(nuevoJugador.getNombre());
                        j.setPosicionX(nuevoJugador.getPosicionX());
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado && mapaIndices.containsKey(nuevoJugador.getId())) {
                    jugadoresActuales.add(new Jugador(nuevoJugador.getNombre(),
                            nuevoJugador.getPosicionX(),
                            nuevoJugador.getId()));
                }
            }

            jugadoresConectados = jugadoresActuales.size();
        }

        Platform.runLater(() -> {
            synchronized (jugadoresActuales) {
                for (Jugador jugador : jugadoresActuales) {
                    int idx = obtenerIndicePorId(jugador.getId());
                    if (idx >= 0 && idx < imagenCamellos.length) {
                        imagenCamellos[idx].setX(jugador.getPosicionX());
                    }
                }
            }

            if (datos.getGanador() != null) {
                texto.setText("¡Ganó: " + datos.getGanador() + "!");
                iniciarCarreraButton.setDisable(true);
                carreraTerminada = true;
                if (timer != null) timer.stop();
            } else {
                actualizarBotonIniciar();
            }
        });
    }

    private void actualizarBotonIniciar() {
        iniciarCarreraButton.setDisable(jugadoresConectados < TAM_GRUPO);
        if (jugadoresConectados == TAM_GRUPO) {
            texto.setText("¡Todos los jugadores conectados! Listos para iniciar.");
        } else {
            texto.setText("Esperando jugadores: " + jugadoresConectados + "/" + TAM_GRUPO);
        }
    }

    private void actualizarJugadorLocal(String id, double nuevaPosicion) {
        synchronized (jugadoresActuales) {
            boolean encontrado = false;
            for (Jugador j : jugadoresActuales) {
                if (j.getId().equals(id)) {
                    j.setNombre(nombrePropioCamello);
                    j.setPosicionX(nuevaPosicion);
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado && mapaIndices.containsKey(id)) {
                jugadoresActuales.add(new Jugador(nombrePropioCamello, nuevaPosicion, id));
            }
            jugadoresConectados = jugadoresActuales.size();
        }
    }

    private int obtenerIndicePorId(String id) {
        Integer idx = mapaIndices.get(id);
        return idx == null ? -1 : idx;
    }

    private void asignarIndiceSiHaceFalta(String idJugador) {
        if (idJugador == null) return;
        if (mapaIndices.containsKey(idJugador)) return;
        if (siguienteSlot >= TAM_GRUPO) return;

        mapaIndices.put(idJugador, siguienteSlot);
        final int idxAsignado = siguienteSlot;
        siguienteSlot++;

        Platform.runLater(() -> {
            if (idxAsignado >= 0 && idxAsignado < imagenCamellos.length) {
                imagenCamellos[idxAsignado].setX(0.0);
            }
        });

        System.out.println("Asignado slot " + idxAsignado + " a " + idJugador);
    }
}
