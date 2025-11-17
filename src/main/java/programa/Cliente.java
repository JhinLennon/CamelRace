package programa;



import cliente.ClienteTCP;
import mensajes.AsignacionGrupo;
import tarea.camelrace.*;

public class Cliente {

    private static final String HOST_SERVIDOR = "localhost";
    private static final int PUERTO_TCP = 6004;

    public static void main(String[] args) {
        try {

            ClienteTCP clienteTCP = new ClienteTCP("jugador01", HOST_SERVIDOR);
            AsignacionGrupo asignacion = clienteTCP.conectar();


            CamelController controlador = new CamelController();
            ClienteMulticastUDP clienteUDP = new ClienteMulticastUDP(
                    "jugador01",
                    asignacion.ipMulticast,
                    asignacion.puerto
            );


            clienteUDP.setControlador(controlador);

            clienteUDP.iniciar();

            Launcher.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
