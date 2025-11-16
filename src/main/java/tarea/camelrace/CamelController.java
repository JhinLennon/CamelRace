package tarea.camelrace;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;

public class CamelController {

    @FXML private Label texto;
    @FXML private Button iniciarCarreraButton;
    @FXML private ImageView imagenCamellos1;

    private final double META_X = 750.0; // Límite claro en X=750
    private AnimationTimer timer;

    @FXML
    public void initialize() {
        System.out.println("Meta establecida en X=" + META_X);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                verificarMeta();
            }
        };
    }

    @FXML
    protected void onIniciarCarreraClick(ActionEvent event) {
        texto.setText("¡Corriendo hacia X=750!");
        iniciarCarreraButton.setText("Acelerar");

        // Iniciar verificación
        timer.start();

        // Cambiar acción del botón
        iniciarCarreraButton.setOnAction(e -> {
            acelerarCamello();
        });
    }

    private void acelerarCamello() {
        double velocidad = (Math.random() * (30 - 10) + 10);
        double nuevaPosicion = imagenCamellos1.getX() + velocidad;
        imagenCamellos1.setX(nuevaPosicion);

        System.out.println("Posición: " + nuevaPosicion + "/" + META_X);

        // Verificar inmediatamente después de mover
        if (nuevaPosicion >= META_X) {
            carreraTerminada();
        }
    }

    private void verificarMeta() {
        if (imagenCamellos1.getX() >= META_X) {
            carreraTerminada();
        }
    }

    private void carreraTerminada() {
        iniciarCarreraButton.setDisable(true);
        texto.setText("¡Llegó a X=750! Carrera terminada.");
        timer.stop();

        System.out.println("¡Meta alcanzada! Botón deshabilitado.");
    }
}
