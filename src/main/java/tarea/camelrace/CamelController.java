package tarea.camelrace;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.event.ActionEvent;
public class CamelController {
    @FXML
    public Button iniciarCarreraButton;
    public Pane paneCamellos;
    @FXML
    private Label texto;

    @FXML
    protected void onIniciarCarreraClick(ActionEvent event){

        texto.setText("Iniciando Carrera");
    }
}
