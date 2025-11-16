package programa;

import java.io.Serializable;
import java.util.List;

public class DatosCarrera implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Jugador> jugadores;
    private boolean carreraIniciada;
    private String ganador;

    public DatosCarrera() {}

    public DatosCarrera(List<Jugador> jugadores, boolean carreraIniciada, String ganador) {
        this.jugadores = jugadores;
        this.carreraIniciada = carreraIniciada;
        this.ganador = ganador;
    }

    public List<Jugador> getJugadores() { return jugadores; }
    public void setJugadores(List<Jugador> jugadores) { this.jugadores = jugadores; }
    public boolean isCarreraIniciada() { return carreraIniciada; }
    public void setCarreraIniciada(boolean carreraIniciada) { this.carreraIniciada = carreraIniciada; }
    public String getGanador() { return ganador; }
    public void setGanador(String ganador) { this.ganador = ganador; }

    @Override
    public String toString() {
        return "DatosCarrera{" +
                "jugadores=" + jugadores +
                ", carreraIniciada=" + carreraIniciada +
                ", ganador=" + ganador +
                '}';
    }
}
