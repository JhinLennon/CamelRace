package mensajes;

import java.io.Serializable;

public class AsignacionGrupo implements Serializable {
    private static final long serialVersionUID = 1L;

    public int idGrupo;
    public String ipMulticast;
    public int puerto;
    public int tamGrupo;
    public long semillaCarrera;

    private String idJugador; // Nuevo campo

    public AsignacionGrupo(int id, String ip, int p, int t, long seed) {
        idGrupo = id;
        ipMulticast = ip;
        puerto = p;
        tamGrupo = t;
        semillaCarrera = seed;
    }

    public String getIdJugador() {
        return idJugador;
    }

    public void setIdJugador(String idJugador) {
        this.idJugador = idJugador;
    }
}
