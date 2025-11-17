package programa;



import cliente.ClienteTCP;
import mensajes.AsignacionGrupo;
import tarea.camelrace.*;

public class Cliente {

    private static final String HOST_SERVIDOR = "192.168.113.13";
    public static final String ID = "jugador01";

    public static void main(String[] args) {
        try {

            ClienteTCP clienteTCP = new ClienteTCP(ID, HOST_SERVIDOR);
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
