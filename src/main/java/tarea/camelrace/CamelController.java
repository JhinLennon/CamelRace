package tarea.camelrace;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import programa.ClienteMulticastUDP;
import programa.DatosCarrera;
import programa.Jugador;

import java.util.ArrayList;
import java.util.List;

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
    private String idJugador = "jugador_1";
    private boolean carreraTerminada = false;
    private List<Jugador> jugadoresActuales = new ArrayList<>();
    private int jugadoresConectados = 0;

    @FXML
    public void initialize() {
        imagenCamellos = new ImageView[] {
                imagenCamellos1,
                imagenCamellos2,
                imagenCamellos3,
                imagenCamellos4
        };
        iniciarCarreraButton.setDisable(true);
    }

    public void setClienteMulticastUDP(ClienteMulticastUDP cliente) {
        this.clienteMulticastUDP = cliente;
        cliente.setControlador(this);
    }

    @FXML
    public void conectar() {
        if (nombreCamello.getText().trim().isEmpty()) {
            texto.setText("Por favor, introduce un nombre.");
            return;
        }
        nombrePropioCamello = nombreCamello.getText().trim();
        conectarButton.setDisable(true);
        nombreCamello.setDisable(true);
        texto.setText("Conectado como: " + nombrePropioCamello);
        iniciarCarreraButton.setDisable(false);
    }

    @FXML
    protected void onIniciarCarreraClick(javafx.event.ActionEvent event) {
        if (jugadoresConectados < 4) {
            texto.setText("Esperando a 4 jugadores...");
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
        double velocidad = (Math.random() * (30 - 10) + 10);
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
        if (imagenCamellos[idx].getX() >= META_X) {
            carreraTerminada = true;
            timer.stop();
            carreraTerminada();
        }
    }

    private void carreraTerminada() {
        iniciarCarreraButton.setDisable(true);
        texto.setText("¡Meta alcanzada!");
        if (timer != null) timer.stop();
        carreraTerminada = true;
    }

    public void actualizarDatosJugadores(DatosCarrera datos) {
        jugadoresActuales = datos.getJugadores();
        jugadoresConectados = datos.getJugadores().size();

        for (Jugador jugador : datos.getJugadores()) {
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
            iniciarCarreraButton.setDisable(jugadoresConectados < 4);
        }
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
        switch(id) {
            case "jugador_1": return 0;
            case "jugador_2": return 1;
            case "jugador_3": return 2;
            case "jugador_4": return 3;
            default: return -1;
        }
    }
}
