package mensajes;

import java.io.Serializable;

public class SolicitudConexion implements Serializable {
    public String idCliente;

    public SolicitudConexion(String id) {
        this.idCliente = id;
    }
}
