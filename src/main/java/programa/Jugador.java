package programa;

import java.io.Serializable;

public class Jugador implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private double posicionX;
    private String id;

    public Jugador(String nombre, double posicionX, String id) {
        this.nombre = nombre;
        this.posicionX = posicionX;
        this.id = id;
    }

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.posicionX = 0.0;
        this.id = "temp";
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getPosicionX() { return posicionX; }
    public void setPosicionX(double posicionX) { this.posicionX = posicionX; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return nombre + " [X:" + posicionX + ", ID:" + id + "]";
    }
}
