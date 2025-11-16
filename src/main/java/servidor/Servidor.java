package servidor;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Servidor {


    private static final int PORT = 6004;
    private static final int TAM_GRUPO = 4;


    private ServerSocket servidor;
    private List<Socket> clientesPendientes = Collections.synchronizedList(new ArrayList<>());


    private Semaphore semIdGrupo = new Semaphore(1);
    private int nextGroupId = 1;


    // Lista de IP multicast predefinidas (puedes ampliarlas)
    private List<String> multicastDisponibles = Arrays.asList(
            "230.0.0.1", "230.0.0.2", "230.0.0.3"
    );


    private int indiceMulticast = 0;


    public void iniciar() throws Exception {
        servidor = new ServerSocket(PORT);
        System.out.println("[SERVER] Servidor escuchando en puerto " + PORT);


        while (true) {
            Socket cliente = servidor.accept();
            System.out.println("[SERVER] Nuevo cliente conectado: " + cliente.getInetAddress());
            new Thread(() -> gestionarCliente(cliente)).start();
        }
    }


    private void gestionarCliente(Socket cliente) {
        try {
            synchronized (clientesPendientes) {
                clientesPendientes.add(cliente);
                System.out.println("[SERVER] Cliente añadido. Total: " + clientesPendientes.size());


                if (clientesPendientes.size() == TAM_GRUPO) {
                    crearGrupo();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void crearGrupo() throws Exception {
        List<Socket> grupo = new ArrayList<>(clientesPendientes);
        clientesPendientes.clear();


        semIdGrupo.acquire();
        int idGrupo = nextGroupId++;
        semIdGrupo.release();
    }

    public static void main(String[] args) {
        try {
            new Servidor().iniciar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}