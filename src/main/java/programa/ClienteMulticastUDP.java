//TODO: crear hilos para la entrada y salida de paquetes
import java.io.*;
import java.net.*;

public class ClienteMulticastUDP {
    private String nombreUsuario;
    private String multicastIP;
    private int multicastPort;
    private MulticastSocket socket;
    private boolean conectado;
    InetAddress grupo =null;

    public ClienteMulticastUDP(String nombreUsuario){
        this.nombreUsuario=nombreUsuario;
    }
    public ClienteMulticastUDP(String nombreUsuario, String multicastIP, int multicastPort){
        this.nombreUsuario = nombreUsuario;
        this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
    }

    public void iniciar() {
        SocketAddress sockadd=null;
        NetworkInterface netIf=null;
        try {
            if (multicastIP == null || multicastPort == 0) {
                if (!obtenerGrupoDelServidor()) {
                    System.err.println("No se pudo obtener el grupo multicast del servidor");
                    return;
                }
            }

            // Unirse al grupo multicast
            socket = new MulticastSocket(multicastPort);
            InetAddress grupo = InetAddress.getByName(multicastIP);

            sockadd=new InetSocketAddress(grupo,multicastPort);
            netIf=NetworkInterface.getByInetAddress(InetAddress.getByName("DESKTOP"));//cambiarlo por su hostname
            socket.joinGroup(sockadd,netIf);
            conectado = true;

            System.out.println("Conectado al grupo " + multicastIP + ":" + multicastPort);
            // Enviar mensajes
            enviarMensajes(grupo);
            //  recibir mensajes
            recibirMensajes();

            

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            desconectar(sockadd,netIf);
        }
    }

    //intento de conexion tcp para la prueba de la clase(se quitaria al tener la clase de marcos)
    private boolean obtenerGrupoDelServidor() {
        System.out.println("Conectando al servidor para obtener grupo multicast...");

        String serverHost="localhost";
        int serverPuerto=8080;

        try (Socket serverSocket = new Socket(serverHost, serverPuerto);
             BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));) {

            // Leer mensaje inicial del servidor
            String respuesta = in.readLine();
            System.out.println("Servidor: " + respuesta);

            if (respuesta.startsWith("WAITING")) {
                System.out.println("Esperando asignación de grupo multicast...");

                // Esperar la asignación del grupo
                while ((respuesta = in.readLine()) != null) {
                    System.out.println("Servidor: " + respuesta);

                    if (respuesta.startsWith("GROUP_ASSIGNED:")) {
                        // Parsear la información del grupo multicast
                        // Formato: GROUP_ASSIGNED:230.0.0.1:50001
                        String[] partes = respuesta.split(":");
                        if (partes.length >= 3) {
                            this.multicastIP = partes[1];
                            this.multicastPort = Integer.parseInt(partes[2]);

                            System.out.println("Grupo asignado: " + multicastIP + ":" + multicastPort);
                            return true;
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error conectando al servidor: " + e.getMessage());
        }

        return false;
    }

    //recibe paquetes de objetos para luego printarlos
    private void recibirMensajes() {
        byte[] buffer = new byte[1024];
        while (conectado) {
            try {
                DatagramPacket paqueteRecibir = new DatagramPacket(buffer, buffer.length);
                socket.receive(paqueteRecibir);

                // Convertir bytes a objeto Persona
                ByteArrayInputStream bais = new ByteArrayInputStream(paqueteRecibir.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Mensaje msg = (Mensaje) ois.readObject();
                ois.close();

                System.out.println(msg);
            } catch (IOException|ClassNotFoundException e) {
                if (conectado) System.err.println("Error recibiendo: " + e.getMessage());
            }
        }
    }

    //envia paquetes de objetos para luego printarlos
    private void enviarMensajes(InetAddress grupo) {
        try(BufferedReader in=new BufferedReader(new InputStreamReader(System.in))) {

            while (conectado) {
                String msj=in.readLine();
                Mensaje msg=new Mensaje(msj);
                if (msj.equals("salir")) break;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(msg);
                oos.close();

                byte[] buf = baos.toByteArray();

                // Enviar datagrama al servidor
                DatagramPacket paquete = new DatagramPacket(buf, buf.length,grupo, multicastPort);

                socket.send(paquete);
            }
        } catch (IOException e) {
            System.err.println("Error enviando: " + e.getMessage());
        }
    }

    private void desconectar(SocketAddress sockadd, NetworkInterface netIf) {
        conectado = false;
        try {
            if (socket != null) {
                socket.leaveGroup(sockadd,netIf);
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error desconectando: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception{

        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Cliente usuario: ");
        String nombre = in.readLine();
        ClienteMulticastUDP cliente;

        if (args.length >= 3) {
            // Usar grupo multicast proporcionado
            String ip = args[1];
            int puerto = Integer.parseInt(args[2]);
            cliente = new ClienteMulticastUDP(nombre, ip, puerto);
        } else {
            // Obtener grupo multicast del servidor (esto deberia de ir conectado a la clase que esta haciendo marcos para conseguir la ip y puerto)
            cliente = new ClienteMulticastUDP(nombre);
        }

        cliente.iniciar();
    }
}

//clase de prueba se tendria que cambiar por un objeto que envie el usuario y la pasicion
import java.io.Serializable;

public class Mensaje implements Serializable {
    String mensaje;

    public Mensaje(String mensaje) {
        this.mensaje=mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    @Override
    public String toString(){
        return mensaje;
    }
}
