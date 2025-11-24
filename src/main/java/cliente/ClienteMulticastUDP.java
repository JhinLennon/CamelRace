package cliente;

import javafx.application.Platform;
import mensajes.DatosCarrera;
import tarea.camelrace.CamelController;

import java.io.*;
import java.net.*;
import java.util.Collections;

public class ClienteMulticastUDP {

    private String nombreUsuario;
    private String multicastIP;
    private int multicastPort;
    private MulticastSocket socket;
    private boolean conectado;
    private InetAddress grupo;
    private CamelController controlador;

    public ClienteMulticastUDP(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public ClienteMulticastUDP(String nombreUsuario, String multicastIP, int multicastPort) {
        this.nombreUsuario = nombreUsuario;
        this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
    }

    public void setControlador(CamelController controlador) {
        this.controlador = controlador;
    }

    public void iniciar() {
        try {
            socket = new MulticastSocket(multicastPort);
            grupo = InetAddress.getByName(multicastIP);

            // Seleccionar interfaz REAL (no loopback)
            NetworkInterface netIf = null;
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.supportsMulticast() && ni.isUp() && !ni.isLoopback()) {
                    netIf = ni;
                    break;
                }
            }

            if (netIf == null) {
                throw new IOException("No hay interfaz válida para multicast.");
            }

            System.out.println("Usando interfaz multicast: " + netIf.getDisplayName());

            // Muy importante:
            socket.joinGroup(grupo);

            conectado = true;

            System.out.println("Conectado al grupo " + multicastIP + ":" + multicastPort);

            Thread hiloRecepcion = new Thread(this::recibirMensajes);
            hiloRecepcion.setDaemon(true);
            hiloRecepcion.start();

        } catch (IOException e) {
            System.err.println("Error iniciando cliente multicast: " + e.getMessage());
            conectado = false;
            if (socket != null) socket.close();
        }
    }


    private void recibirMensajes() {
        byte[] buffer = new byte[4096];
        while (conectado) {
            try {
                DatagramPacket paqueteRecibir = new DatagramPacket(buffer, buffer.length);
                socket.receive(paqueteRecibir);

                ByteArrayInputStream bais = new ByteArrayInputStream(paqueteRecibir.getData(), 0, paqueteRecibir.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                DatosCarrera datos = (DatosCarrera) ois.readObject();
                ois.close();

                System.out.println("Datos recibidos: " + datos);

                if (controlador != null) {
                    // Actualizar UI en hilo aplicación JavaFX
                    Platform.runLater(() -> controlador.actualizarDatosJugadores(datos));
                }

            } catch (IOException | ClassNotFoundException e) {
                if (conectado) System.err.println("Error recibiendo: " + e.getMessage());
                // Considerar romper el ciclo o reconectar si fuera necesario
            }
        }
    }

    public void enviarDatosCarrera(DatosCarrera datos) {
        if (!conectado || grupo == null) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(datos);
            oos.close();

            byte[] buf = baos.toByteArray();
            DatagramPacket paquete = new DatagramPacket(buf, buf.length, grupo, multicastPort);
            socket.send(paquete);

            System.out.println("Enviado: " + datos);
        } catch (IOException e) {
            System.err.println("Error enviando datos: " + e.getMessage());
        }
    }

    public void desconectar() {
        conectado = false;
        try {
            if (socket != null && grupo != null) {
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                SocketAddress sockadd = new InetSocketAddress(grupo, multicastPort);
                socket.leaveGroup(sockadd, netIf);
                socket.close();
                System.out.println("Desconectado del grupo " + multicastIP);
            }
        } catch (IOException e) {
            System.err.println("Error desconectando: " + e.getMessage());
        }
    }
}
