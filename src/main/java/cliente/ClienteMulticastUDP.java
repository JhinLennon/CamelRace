package cliente;

import javafx.application.Platform;
import mensajes.DatosCarrera;
import tarea.camelrace.CamelController;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;

public class ClienteMulticastUDP {

    private String nombreUsuario;
    private String multicastIP;
    private int multicastPort;
    private MulticastSocket socket;
    private boolean conectado;
    private InetAddress grupo;
    private CamelController controlador;

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
            grupo = InetAddress.getByName(multicastIP);

            socket = new MulticastSocket(multicastPort);

            // TTL más alto (evita redes que bloquean TTL=1)
            socket.setTimeToLive(16);

            // Seleccionar la interfaz física correcta
            NetworkInterface netIf = elegirInterfazMulticast();

            if (netIf == null) {
                throw new IOException("No se encontró una interfaz válida para multicast");
            }

            System.out.println("→ Usando interfaz: " + netIf.getDisplayName());

            socket.setNetworkInterface(netIf);

            SocketAddress sockAddr = new InetSocketAddress(grupo, multicastPort);

            socket.joinGroup(sockAddr, netIf);

            conectado = true;
            System.out.println("Conectado al grupo multicast " + multicastIP + ":" + multicastPort);

            // Hilo para escuchar mensajes
            Thread hilo = new Thread(this::recibirMensajes);
            hilo.setDaemon(true);
            hilo.start();

        } catch (IOException e) {
            System.err.println("Error iniciando cliente multicast: " + e.getMessage());
            conectado = false;
            if (socket != null) socket.close();
        }
    }

    /**
     * Selecciona la interfaz de red adecuada para multicast.
     */
    private NetworkInterface elegirInterfazMulticast() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();

        while (ifaces.hasMoreElements()) {
            NetworkInterface ni = ifaces.nextElement();

            try {
                if (!ni.isUp()) continue;
                if (ni.isLoopback()) continue;
                if (!ni.supportsMulticast()) continue;
                if (ni.isVirtual()) continue;

                // Recorre sus direcciones IP
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.113.")) {
                            System.out.println("Interfaz multicast seleccionada: " + ni.getDisplayName() + " (" + ip + ")");
                            return ni;
                        }
                    }
                }

            } catch (Exception ignored) {}
        }
        return null;
    }



    private void recibirMensajes() {
        byte[] buffer = new byte[4096];

        while (conectado) {
            try {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socket.receive(paquete);

                ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(paquete.getData(), 0, paquete.getLength())
                );

                DatosCarrera datos = (DatosCarrera) ois.readObject();
                ois.close();

                System.out.println("Recibido UDP: " + datos);

                if (controlador != null) {
                    Platform.runLater(() -> controlador.actualizarDatosJugadores(datos));
                }

            } catch (IOException | ClassNotFoundException e) {
                if (conectado)
                    System.err.println("Error recibiendo multicast: " + e.getMessage());
            }
        }
    }

    public void enviarDatosCarrera(DatosCarrera datos) {
        if (!conectado) return;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(datos);
            oos.close();

            byte[] buf = baos.toByteArray();

            DatagramPacket paquete = new DatagramPacket(buf, buf.length, grupo, multicastPort);

            socket.send(paquete);

            System.out.println("Enviado UDP: " + datos);

        } catch (IOException e) {
            System.err.println("Error enviando UDP: " + e.getMessage());
        }
    }

    public void desconectar() {
        conectado = false;

        try {
            if (socket != null) {
                NetworkInterface netIf = socket.getNetworkInterface();

                if (netIf != null && grupo != null) {
                    socket.leaveGroup(new InetSocketAddress(grupo, multicastPort), netIf);
                }

                socket.close();
                System.out.println("Desconectado de multicast");
            }

        } catch (Exception e) {
            System.err.println("Error al desconectar multicast: " + e.getMessage());
        }
    }
}
