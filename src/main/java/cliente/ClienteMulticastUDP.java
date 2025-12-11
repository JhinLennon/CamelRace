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
    private NetworkInterface networkInterface;

    public ClienteMulticastUDP(String idJugador, String multicastIP, int port) {
        this.idJugador = idJugador;
        this.multicastIP = multicastIP;
        this.multicastPort = port;
    }

    public void setControlador(CamelController controlador) {
        this.controlador = controlador;
    }

    // ============================================================
    // Detecta INTERFAZ 192.168.113.X (tu red local del aula)
    // ============================================================
    private NetworkInterface obtenerInterfazCorrecta() throws Exception {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();

            if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast())
                continue;

            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address &&
                        addr.getHostAddress().startsWith("192.168.113.")) {

                    System.out.println("[UDP] Usando interfaz: " + ni.getDisplayName());
                    return ni;
                }
            }
        }

        // Alternativa si no encuentra 192.168.113.X
        nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();
            if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) continue;

            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address && !addr.isLinkLocalAddress()) {
                    System.out.println("[UDP] Interfaz alternativa: " + ni.getDisplayName());
                    return ni;
                }
            }
        }

        throw new RuntimeException("No se encontró interfaz válida para multicast");
    }

    // ============================================================
    // Iniciar Multicast con loopback ACTIVADO
    // ============================================================
    public void iniciar() {
        try {
            grupo = InetAddress.getByName(multicastIP);

            networkInterface = obtenerInterfazCorrecta();

            socket = new MulticastSocket(multicastPort);

            // CONFIGURACIONES IMPORTANTES
            socket.setReuseAddress(true);
            socket.setTimeToLive(32);
            socket.setSoTimeout(0);

            // *** REPARADO ***
            socket.setLoopbackMode(false); // <--- ACTIVA loopback correctamente

            InetAddress ipLocal = null;
            for (InetAddress addr : Collections.list(networkInterface.getInetAddresses())) {
                if (addr instanceof Inet4Address) {
                    ipLocal = addr;
                    break;
                }
            }

            System.out.println("[UDP] IP local: " + (ipLocal != null ? ipLocal.getHostAddress() : "N/A"));

            // Unirse al grupo
            socket.joinGroup(new InetSocketAddress(grupo, multicastPort), networkInterface);

            conectado = true;
            System.out.println("[UDP] Conectado a " + multicastIP + ":" + multicastPort);

            // Hilo receptor
            Thread hilo = new Thread(this::recibirMensajes);
            hilo.setDaemon(true);
            hilo.start();

            enviarMensajePrueba();

        } catch (Exception e) {
            System.err.println("[UDP] Error iniciando: " + e.getMessage());
            e.printStackTrace();
            if (socket != null) socket.close();
        }
    }

    // ============================================================
    // Mensaje de prueba
    // ============================================================
    private void enviarMensajePrueba() {
        try {
            String mensaje = "CONEXION:" + idJugador;
            byte[] buffer = mensaje.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, grupo, multicastPort);
            socket.send(packet);
        } catch (Exception ignored) {}
    }

    // ============================================================
    // Enviar DatosCarrera como objeto
    // ============================================================
    public void enviarDatosCarrera(DatosCarrera datos) {
        if (!conectado) return;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(datos);
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length, grupo, multicastPort);

            socket.send(packet);

        } catch (IOException e) {
            System.err.println("[UDP] Error enviando datos: " + e.getMessage());
        }
    }

    // ============================================================
    // Recibir mensajes MULTICAST
    // ============================================================
    private void recibirMensajes() {
        byte[] buffer = new byte[65535];

        while (conectado && !socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(packet);

                ByteArrayInputStream bais =
                        new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Object obj = ois.readObject();

                if (obj instanceof DatosCarrera datos) {
                    if (controlador != null) {
                        controlador.actualizarDatosJugadores(datos);
                    }
                }

            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // Desconectar
    // ============================================================
    public void desconectar() {
        conectado = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.leaveGroup(new InetSocketAddress(grupo, multicastPort), networkInterface);
                socket.close();
            }
        } catch (Exception ignored) {}
    }

    public boolean estaConectado() {
        return conectado;
    }
}
