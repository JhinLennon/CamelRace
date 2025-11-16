package cliente;

import mensajes.AsignacionGrupo;
import mensajes.SolicitudConexion;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class ClienteTCP {


    private String idCliente;
    private String hostServidor;


    public ClienteTCP(String id, String host) {
        this.idCliente = id;
        this.hostServidor = host;
    }


    public AsignacionGrupo conectar() throws Exception {
        Socket socket = new Socket(hostServidor, 6004);
        System.out.println("[CLIENT] Conectado al servidor TCP");


        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
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


    public static void main(String[] args) {
        try {
            ClienteTCP c = new ClienteTCP("jugador01", "localhost");
            AsignacionGrupo datos = c.conectar();
// Luego se pasa "datos" al módulo UDP
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
