package cliente;

import mensajes.AsignacionGrupo;
import mensajes.SolicitudConexion;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClienteTCP {
    public static final int PORT = 6004;
    private String idCliente;
    private String hostServidor;

    public ClienteTCP(String id, String host) {
        this.idCliente = id;
        this.hostServidor = host;
    }

    public AsignacionGrupo conectar() throws Exception {
        Socket socket = new Socket(hostServidor, PORT);
        System.out.println("[CLIENT] Conectado al servidor TCP");

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(new SolicitudConexion(idCliente));
        out.flush();

        System.out.println("[CLIENT] Esperando asignación de grupo...");
        AsignacionGrupo asignacion = (AsignacionGrupo) in.readObject();

        System.out.println("[CLIENT] Asignado al grupo " + asignacion.idGrupo);
        System.out.println("Multicast: " + asignacion.ipMulticast + ":" + asignacion.puerto);

        socket.close();
        return asignacion;
    }
}
