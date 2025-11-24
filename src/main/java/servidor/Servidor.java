package servidor;

import mensajes.AsignacionGrupo;
import mensajes.SolicitudConexion;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Servidor {

    private static final int PORT = 6004;
    public static final int TAM_GRUPO = 4;

    private ServerSocket servidor;
    private List<Socket> clientesPendientes = Collections.synchronizedList(new ArrayList<>());

    private Semaphore semIdGrupo = new Semaphore(1);
    private int nextGroupId = 1;

    private List<String> multicastDisponibles = Arrays.asList(
            "231.0.0.1", "231.0.0.2", "231.0.0.3"
    );
    private int indiceMulticast = 0;

    private final String[] JUGADORES_IDS = {
            "jugador_1", "jugador_2", "jugador_3", "jugador_4"
    };

    private ExecutorService pool = Executors.newFixedThreadPool(10);

    public void iniciar() throws Exception {
        servidor = new ServerSocket(PORT);
        System.out.println("[SERVER] Servidor escuchando en puerto " + PORT);

        try {
            while (true) {
                Socket cliente = servidor.accept();
                System.out.println("[SERVER] Nuevo cliente conectado: " + cliente.getInetAddress());
                pool.execute(() -> gestionarCliente(cliente));
            }
        } finally {
            if (servidor != null) servidor.close();
            pool.shutdown();
        }
    }

    private void gestionarCliente(Socket cliente) {
        synchronized (clientesPendientes) {
            clientesPendientes.add(cliente);
            System.out.println("[SERVER] Cliente añadido. Total: " + clientesPendientes.size());

            if (clientesPendientes.size() == TAM_GRUPO) {
                try {
                    crearGrupo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void crearGrupo() throws Exception {
        List<Socket> grupo = new ArrayList<>(clientesPendientes);
        clientesPendientes.clear();

        semIdGrupo.acquire();
        int idGrupo = nextGroupId++;
        semIdGrupo.release();

        String ipMulticast = multicastDisponibles.get(indiceMulticast);
        indiceMulticast = (indiceMulticast + 1) % multicastDisponibles.size();

        int puertoMulticast = 5000 + idGrupo;
        long semillaCarrera = System.currentTimeMillis();

        for (int i = 0; i < grupo.size(); i++) {
            Socket cliente = grupo.get(i);
            final String idJugador = JUGADORES_IDS[i];

            pool.execute(() -> {
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                try {
                    out = new ObjectOutputStream(cliente.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(cliente.getInputStream());

                    SolicitudConexion solicitud = (SolicitudConexion) in.readObject();
                    System.out.println("[SERVER] Recibida Solicitud de: " + solicitud.idCliente);

                    AsignacionGrupo asignacion = new AsignacionGrupo(
                            idGrupo, ipMulticast, puertoMulticast, TAM_GRUPO, semillaCarrera
                    );
                    asignacion.setIdJugador(idJugador);

                    out.writeObject(asignacion);
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { if (in != null) in.close(); } catch (Exception ignored) {}
                    try { if (out != null) out.close(); } catch (Exception ignored) {}
                    try { if (cliente != null) cliente.close(); } catch (Exception ignored) {}
                }
            });
        }
    }

    public static void main(String[] args) {
        try {
            new Servidor().iniciar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
