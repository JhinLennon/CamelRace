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

        var params = getParameters();

        // Obtener parámetros nombrados generados por main()
        ID = params.getNamed().getOrDefault("id", "jugador03");
        HOST = params.getNamed().getOrDefault("host", "localhost");

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

        new Thread(clienteUDP::iniciar).start();
    }

    public static void main(String[] args) {

        // Defaults
        String id = "jugador03";
        String host = "localhost";

        // Si el usuario pasa parámetros posicionales, sobrescribirlos
        if (args.length > 0) id = args[0];
        if (args.length > 1) host = args[1];

        // Transformarlos a argumentos que JavaFX detecta
        launch("--id=" + id, "--host=" + host);
    }
}
