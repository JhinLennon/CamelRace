package programa;
import tarea.camelrace.*;
import cliente.ClienteTCP;
import mensajes.AsignacionGrupo;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tarea.camelrace.CamelController;

public class Cliente extends Application {

    public static final String ID = "jugador04";
    public static final String HOST = "localhost";
    private ClienteMulticastUDP clienteUDP;
    private CamelController controlador;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Conectar vía TCP y obtener datos multicast
        ClienteTCP clienteTCP = new ClienteTCP(ID, HOST);
        AsignacionGrupo asignacion = clienteTCP.conectar();

        // Cargar interfaz FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tarea/camelrace/camel-view.fxml"));
        Parent root = loader.load();

        // Obtener controlador real
        controlador = loader.getController();

        // Crear el cliente UDP con los datos obtenidos
        clienteUDP = new ClienteMulticastUDP(ID, asignacion.ipMulticast, asignacion.puerto);

        // Pasar cliente UDP al controlador
        controlador.setClienteMulticastUDP(clienteUDP);

        // Configurar y mostrar la ventana
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Camel Race");
        primaryStage.show();

        // Iniciar el cliente UDP en un hilo independiente para no bloquear la UI
        new Thread(() -> clienteUDP.iniciar()).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
