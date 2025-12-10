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
    private NetworkInterface networkInterface; // GUARDAR la interfaz

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
    private NetworkInterface obtenerInterfazCorrecta() throws Exception {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();

            // Verificar que sea válida para multicast
            if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) {
                System.out.println("[UDP] Descartando interfaz: " + ni.getDisplayName());
                continue;
            }

            System.out.println("[UDP] Analizando interfaz: " + ni.getDisplayName());

            // Buscar IP 192.168.113.X
            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address &&
                        addr.getHostAddress().startsWith("192.168.113.")) {

                    System.out.println("[UDP] ✅ Interfaz encontrada: " +
                            ni.getDisplayName() + " - IP: " + addr.getHostAddress());
                    return ni;
                }
            }
        }

        // Si no encuentra 192.168.113.X, usar cualquier interfaz con IPv4
        System.out.println("[UDP] ⚠️ No se encontró 192.168.113.X, buscando alternativa...");
        nets = NetworkInterface.getNetworkInterfaces();

        while (nets.hasMoreElements()) {
            NetworkInterface ni = nets.nextElement();

            if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) continue;

            for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                if (addr instanceof Inet4Address && !addr.isLinkLocalAddress()) {
                    System.out.println("[UDP] ✅ Usando interfaz alternativa: " +
                            ni.getDisplayName() + " - IP: " + addr.getHostAddress());
                    return ni;
                }
            }
        }

        throw new RuntimeException("No se encontró interfaz de red válida para multicast");
    }

    // ============================================================
    // Iniciar cliente multicast CORREGIDO
    // ============================================================
    public void iniciar() {
        try {
            grupo = InetAddress.getByName(multicastIP);

            // 1. Obtener interfaz de red
            networkInterface = obtenerInterfazCorrecta();

            // 2. Crear socket CON CONFIGURACIÓN CRÍTICA
            socket = new MulticastSocket(multicastPort); // NO bindear a IP específica

            // 3. CONFIGURACIONES IMPRESCINDIBLES
            socket.setReuseAddress(true); // ✅ PERMITE MÚLTIPLES RECEPTORES
            socket.setTimeToLive(32);     // ✅ TTL suficiente para red local
            socket.setSoTimeout(0);       // ✅ Sin timeout
            socket.setLoopbackMode(true); // ✅ Recibir nuestros propios mensajes (para debug)

            // 4. Obtener IP local para mostrar (opcional)
            InetAddress ipLocal = null;
            for (InetAddress addr : Collections.list(networkInterface.getInetAddresses())) {
                if (addr instanceof Inet4Address) {
                    ipLocal = addr;
                    break;
                }
            }

            System.out.println("[UDP] Usando interfaz: " + networkInterface.getDisplayName());
            System.out.println("[UDP] IP local: " + (ipLocal != null ? ipLocal.getHostAddress() : "N/A"));
            System.out.println("[UDP] TTL: " + socket.getTimeToLive());

            // 5. UNIRSE AL GRUPO CORRECTAMENTE (ESTA ES LA CLAVE)
            socket.joinGroup(new InetSocketAddress(grupo, multicastPort), networkInterface);

            conectado = true;
            System.out.println("[UDP] ✅ Conectado al grupo " + multicastIP + ":" + multicastPort);

            // 6. Iniciar hilo receptor
            Thread hilo = new Thread(this::recibirMensajes);
            hilo.setName("Multicast-Receiver-" + idJugador);
            hilo.setDaemon(true);
            hilo.start();

            // 7. Enviar mensaje de prueba (opcional)
            enviarMensajePrueba();

        } catch (Exception e) {
            System.err.println("[UDP] ❌ Error iniciando cliente multicast: " + e.getMessage());
            e.printStackTrace();
            if (socket != null) socket.close();
            conectado = false;
        }
    }

    // ============================================================
    // Enviar mensaje de prueba al conectar
    // ============================================================
    private void enviarMensajePrueba() {
        try {
            String mensajePrueba = "CONEXION:" + idJugador + ":" +
                    InetAddress.getLocalHost().getHostName();
            byte[] buffer = mensajePrueba.getBytes("UTF-8");

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, grupo, multicastPort
            );
            socket.send(packet);

            System.out.println("[UDP] Mensaje de prueba enviado: " + mensajePrueba);
        } catch (Exception e) {
            System.err.println("[UDP] Error enviando mensaje prueba: " + e.getMessage());
        }
    }

    // ============================================================
    // Enviar DatosCarrera por UDP
    // ============================================================
    public void enviarDatosCarrera(DatosCarrera datos) {
        if (!conectado || socket == null || socket.isClosed()) {
            System.err.println("[UDP] No conectado, no se puede enviar");
            return;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(datos);
            oos.flush();
            byte[] buffer = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, grupo, multicastPort
            );
            socket.send(packet);

            System.out.println("[UDP] 📤 Datos enviados: " + datos);

        } catch (IOException e) {
            System.err.println("[UDP] ❌ Error enviando datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // Escuchar mensajes - CORREGIDO
    // ============================================================
    private void recibirMensajes() {
        byte[] buffer = new byte[65535];

        System.out.println("[UDP] 🎧 Iniciando recepción de mensajes...");

        while (conectado && !socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(packet);

                // Ignorar nuestros propios mensajes si no quieres procesarlos
                if (packet.getAddress().equals(InetAddress.getLocalHost())) {
                    continue;
                }

                System.out.println("[UDP] 📥 Paquete recibido de: " +
                        packet.getAddress().getHostAddress() + ":" + packet.getPort() +
                        " - Tamaño: " + packet.getLength() + " bytes");

                // Procesar como objeto
                try (ByteArrayInputStream bais = new ByteArrayInputStream(
                        packet.getData(), 0, packet.getLength());
                     ObjectInputStream ois = new ObjectInputStream(bais)) {

                    Object obj = ois.readObject();

                    if (obj instanceof DatosCarrera) {
                        DatosCarrera datos = (DatosCarrera) obj;

                        if (controlador != null) {
                            controlador.actualizarDatosJugadores(datos);
                        }

                        System.out.println("[UDP] ✅ Datos procesados");
                    }
                }

            } catch (SocketTimeoutException e) {
                // Timeout configurado, ignorar
            } catch (EOFException e) {
                System.err.println("[UDP] ⚠️ EOF - Paquete corrupto o vacío");
            } catch (StreamCorruptedException e) {
                System.err.println("[UDP] ⚠️ Stream corrupto - Formato incorrecto");
            } catch (ClassNotFoundException e) {
                System.err.println("[UDP] ❌ Clase no encontrada: " + e.getMessage());
            } catch (IOException e) {
                if (conectado) {
                    System.err.println("[UDP] ❌ Error en recepción: " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        }

        System.out.println("[UDP] Receptor finalizado");
    }

    // ============================================================
    // Desconectar CORREGIDO
    // ============================================================
    public void desconectar() {
        System.out.println("[UDP] Desconectando...");
        conectado = false;

        try {
            if (socket != null && !socket.isClosed()) {
                // Dejar el grupo usando la misma interfaz
                socket.leaveGroup(new InetSocketAddress(grupo, multicastPort), networkInterface);

                // Cerrar socket
                socket.close();
                System.out.println("[UDP] ✅ Desconectado de multicast");
            }
        } catch (IOException e) {
            System.err.println("[UDP] Error al desconectar: " + e.getMessage());
        }
    }

    // ============================================================
    // Método para verificar estado
    // ============================================================
    public boolean estaConectado() {
        return conectado && socket != null && !socket.isClosed();
    }
}