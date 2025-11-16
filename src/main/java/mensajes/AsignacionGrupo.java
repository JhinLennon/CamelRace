package mensajes;

import java.io.Serializable;

public class AsignacionGrupo implements Serializable {
    public int idGrupo;
    public String ipMulticast;
    public int puerto;
    public int tamGrupo;
    public long semillaCarrera;

    public AsignacionGrupo(int id, String ip, int p, int t, long seed) {
        idGrupo = id;
        ipMulticast = ip;
        puerto = p;
        tamGrupo = t;
        semillaCarrera = seed;
    }
}
