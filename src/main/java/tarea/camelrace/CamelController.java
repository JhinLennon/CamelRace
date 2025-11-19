package tarea.camelrace;

import cliente.ClienteTCP;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import mensajes.AsignacionGrupo;
import programa.ClienteMulticastUDP;
import programa.DatosCarrera;
import programa.Jugador;

import java.util.ArrayList;
import java.util.List;

import static servidor.Servidor.TAM_GRUPO;

public class CamelController {

    private static final String[] JUGADORES_IDS = {
            "jugador_1", "jugador_2", "jugador_3", "jugador_4"
    };

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
    private List<Jugador> jugadoresActuales = new ArrayList<>();
    private int jugadoresConectados = 0;

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

    @FXML
    public void conectar() {
        String nombreInput = nombreCamello.getText().trim();
        if (nombreInput.isEmpty()) {
            texto.setText("Por favor, introduce un nombre.");
            return;
        }
        nombrePropioCamello = nombreInput;
        conectarButton.setDisable(true);
        nombreCamello.setDisable(true);
        texto.setText("Conectando...");

        new Thread(() -> {
            try {
                ClienteTCP clienteTCP = new ClienteTCP(nombrePropioCamello, "localhost");
                AsignacionGrupo asignacion = clienteTCP.conectar();
                Platform.runLater(() -> {
                    recibirAsignacionGrupo(asignacion);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    texto.setText("Error al conectar con servidor.");
                    conectarButton.setDisable(false);
                    nombreCamello.setDisable(false);
                });
            }
        }).start();
    }

    public void recibirAsignacionGrupo(AsignacionGrupo asignacion) {
        this.idJugador = asignacion.getIdJugador();
        texto.setText("Conectado como: " + nombrePropioCamello + " (" + idJugador + ")");
        iniciarCarreraButton.setDisable(false);

        // Iniciar cliente multicast UDP con datos recibidos
        iniciarClienteMulticast(asignacion);
    }

    private void iniciarClienteMulticast(AsignacionGrupo asignacion) {
        clienteMulticastUDP = new ClienteMulticastUDP(nombrePropioCamello, asignacion.ipMulticast, asignacion.puerto);
        clienteMulticastUDP.setControlador(this);
        clienteMulticastUDP.iniciar();
    }

    @FXML
    protected void onIniciarCarreraClick() {
        Jugador jugadorInicial = new Jugador(nombrePropioCamello, 0.0, idJugador);
        List<Jugador> jugadoresIniciales = new ArrayList<>();
        jugadoresIniciales.add(jugadorInicial);
        DatosCarrera datosInicial = new DatosCarrera(jugadoresIniciales, false, null);
        clienteMulticastUDP.enviarDatosCarrera(datosInicial);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
        if (idx < 0) return;
        double velocidad = Math.random() * 20 + 10;
        double nuevaPosicion = imagenCamellos[idx].getX() + velocidad;
        imagenCamellos[idx].setX(nuevaPosicion);

        actualizarJugadorLocal(idJugador, nuevaPosicion);

        DatosCarrera datos = new DatosCarrera(jugadoresActuales, true, null);
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
        System.out.println("Datos recibidos: " + datos);

        for (Jugador nuevoJugador : datos.getJugadores()) {
            boolean encontrado = false;
            for (Jugador j : jugadoresActuales) {
                if (j.getId().equals(nuevoJugador.getId())) {
                    j.setNombre(nuevoJugador.getNombre());
                    j.setPosicionX(nuevoJugador.getPosicionX());
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                jugadoresActuales.add(nuevoJugador);
            }
        }

        jugadoresConectados = jugadoresActuales.size();
        System.out.println("Jugadores conectados: " + jugadoresConectados);

        Platform.runLater(() -> {
            for (Jugador jugador : jugadoresActuales) {
                int idx = obtenerIndicePorId(jugador.getId());
                if (idx >= 0) {
                    imagenCamellos[idx].setX(jugador.getPosicionX());
                }
            }

            if (datos.getGanador() != null) {
                texto.setText("¡Ganó: " + datos.getGanador() + "!");
                iniciarCarreraButton.setDisable(true);
                carreraTerminada = true;
                if (timer != null) timer.stop();
            } else {
                iniciarCarreraButton.setDisable(jugadoresConectados < TAM_GRUPO);
            }
        });
    }

    private void actualizarJugadorLocal(String id, double nuevaPosicion) {
        boolean encontrado = false;
        for (Jugador j : jugadoresActuales) {
            if (j.getId().equals(id)) {
                j.setNombre(nombrePropioCamello);
                j.setPosicionX(nuevaPosicion);
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            jugadoresActuales.add(new Jugador(nombrePropioCamello, nuevaPosicion, id));
        }
    }

    private int obtenerIndicePorId(String id) {
        for (int i = 0; i < JUGADORES_IDS.length; i++) {
            if (JUGADORES_IDS[i].equals(id)) return i;
        }
        return -1;
    }
}
