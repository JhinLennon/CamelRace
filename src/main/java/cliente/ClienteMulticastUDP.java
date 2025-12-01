package cliente;

import mensajes.DatosCarrera;
import tarea.camelrace.CamelController;

import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;

public class ClienteMulticastUDP {

    private String idJugador;
    private MulticastSocket socket;
    private InetAddress grupo;
    private int multicastPort;
    private String multicastIP;
    private boolean conectado = false;
    private CamelController controlador;

    public ClienteMulticastUDP(String idJugador, String multicastIP, int port) {
        this.idJugador = idJugador;
        this.multicastIP = multicastIP;
        this.multicastPort = port;
    }

    public void setControlador(CamelController controlador) {
        this.controlador = controlador;
    }

    // ============================================================
    // Detecta IP local física 192.168.113.X
    // ============================================================
    private InetAddress obtenerIPLocalCorrecta() throws Exception {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();

            if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address &&
                        addr.getHostAddress().startsWith("192.168.113.")) {

                    System.out.println("[UDP] IP detectada para multicast: " + addr.getHostAddress());
                    return addr;
                }
            }
        }

        throw new RuntimeException("No se encontró IP válida 192.168.113.X");
    }

    // ============================================================
    // Iniciar cliente multicast (Windows friendly)
    // ============================================================
    public void iniciar() {
        try {
            grupo = InetAddress.getByName(multicastIP);

            InetAddress ipLocal = obtenerIPLocalCorrecta();

            socket = new MulticastSocket(new InetSocketAddress(ipLocal, multicastPort));
            socket.setTimeToLive(16);

            System.out.println("[UDP] Usando IP local: " + ipLocal.getHostAddress());

            socket.joinGroup(new InetSocketAddress(grupo, multicastPort), null);

            conectado = true;
            System.out.println("[UDP] Conectado al grupo " + multicastIP + ":" + multicastPort);

            Thread hilo = new Thread(this::recibirMensajes);
            hilo.setDaemon(true);
            hilo.start();

        } catch (Exception e) {
            System.err.println("[UDP] Error iniciando cliente multicast: " + e.getMessage());
            if (socket != null) socket.close();
        }
    }

    // ============================================================
    // Enviar DatosCarrera por UDP
    // ============================================================
    public void enviarDatosCarrera(DatosCarrera datos) {
        if (!conectado) return;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(datos);
            oos.flush();
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, grupo, multicastPort);
            socket.send(packet);

        } catch (IOException e) {
            System.err.println("[UDP] Error enviando datos: " + e.getMessage());
        }
    }

    // ============================================================
    // Escuchar mensajes
    // ============================================================
    private void recibirMensajes() {
        byte[] buffer = new byte[65535];

        while (conectado) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                try (ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(packet.getData(), 0, packet.getLength()))) {

                    DatosCarrera datos = (DatosCarrera) ois.readObject();

                    if (controlador != null) {
                        controlador.actualizarDatosJugadores(datos);
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                if (conectado)
                    System.err.println("[UDP] Error recibiendo mensajes: " + e.getMessage());
            }
        }
    }

    // ============================================================
    // Desconectar
    // ============================================================
    public void desconectar() {
        conectado = false;

        try {
            if (socket != null) {
                socket.leaveGroup(new InetSocketAddress(grupo, multicastPort), null);
                socket.close();
                System.out.println("[UDP] Desconectado de multicast");
            }
        } catch (IOException e) {
            System.err.println("[UDP] Error al desconectar: " + e.getMessage());
        }
    }
}
