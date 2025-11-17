package programa;

import javafx.application.Platform;
import tarea.camelrace.CamelController;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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
        SocketAddress sockadd = null;
        NetworkInterface netIf = null;
        try {

            socket = new MulticastSocket(multicastPort);
            grupo = InetAddress.getByName(multicastIP);

            sockadd = new InetSocketAddress(grupo, multicastPort);
            netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            socket.joinGroup(sockadd, netIf);
            conectado = true;

            System.out.println("Conectado al grupo " + multicastIP + ":" + multicastPort);

            Thread hiloRecepcion = new Thread(this::recibirMensajes);
            Thread hiloEnvio = new Thread(this::enviarJugadores);

            hiloRecepcion.start();
            hiloEnvio.start();

            hiloRecepcion.join();
            hiloEnvio.join();

        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            desconectar(sockadd, netIf);
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
                    Platform.runLater(() -> controlador.actualizarDatosJugadores(datos));
                }

            } catch (IOException | ClassNotFoundException e) {
                if (conectado) System.err.println("Error recibiendo: " + e.getMessage());
            }
        }
    }

    private void enviarJugadores() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (conectado) {
                System.out.print("Ingrese posición X del jugador (o 'salir' para terminar): ");
                String input = in.readLine();

                if (input.equals("salir")) break;

                try {
                    double posicionX = Double.parseDouble(input);
                    Jugador jugador = new Jugador(nombreUsuario, posicionX, "jugador_" + nombreUsuario);
                    DatosCarrera datos = new DatosCarrera(new ArrayList<>(List.of(jugador)), true, null);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(datos);
                    oos.close();

                    byte[] buf = baos.toByteArray();
                    DatagramPacket paquete = new DatagramPacket(buf, buf.length, grupo, multicastPort);
                    socket.send(paquete);

                    System.out.println("Enviado: " + jugador);

                } catch (NumberFormatException e) {
                    System.out.println("Por favor ingrese un número válido para la posición X");
                }
            }
        } catch (IOException e) {
            System.err.println("Error enviando: " + e.getMessage());
        }

        conectado = false;
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

    private void desconectar(SocketAddress sockadd, NetworkInterface netIf) {
        conectado = false;
        try {
            if (socket != null && sockadd != null && netIf != null) {
                socket.leaveGroup(sockadd, netIf);
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error desconectando: " + e.getMessage());
        }
    }
}
