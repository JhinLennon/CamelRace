package cliente;

import tarea.camelrace.CamelController;
import mensajes.AsignacionGrupo;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Cliente extends Application {

    private String ID;
    private String HOST;

    @Override
    public void start(Stage primaryStage) throws Exception {

        //Intentar obtener ID y HOST desde argumentos
        var params = getParameters();

        ID = params.getNamed().getOrDefault("id", null);
        HOST = params.getNamed().getOrDefault("host", null);

        // Si no vienen por argumentos, intentar por propiedades del sistema
        if (ID == null) ID = System.getProperty("ID");
        if (HOST == null) HOST = System.getProperty("HOST");

        // Si siguen sin llegar, usar valores por defecto o lanzar error
        if (ID == null) ID = "jugador03";
        if (HOST == null) HOST = "localhost";

        System.out.println("Cliente iniciado con:");
        System.out.println("   ID   = " + ID);
        System.out.println("   HOST = " + HOST);

        // Conectar vía TCP
        ClienteTCP clienteTCP = new ClienteTCP(ID, HOST);
        AsignacionGrupo asignacion = clienteTCP.conectar();

        // Cargar interfaz
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tarea/camelrace/camel-view.fxml"));
        Parent root = loader.load();

        // Obtener controlador
        CamelController controlador = loader.getController();

        // Crear cliente UDP
        ClienteMulticastUDP clienteUDP = new ClienteMulticastUDP(ID, asignacion.ipMulticast, asignacion.puerto);

        // Pasar parámetros al controlador
        controlador.setClienteMulticastUDP(clienteUDP);
        controlador.setAsignacionGrupo(asignacion);

        // Mostrar la ventana
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Camel Race");
        primaryStage.show();

        // UDP en un hilo separado
        new Thread(clienteUDP::iniciar).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
