package ingenieria.de.software.sherly.model;

import java.util.List;

public class Camino {
    private List<Nodo> nodos;
    private Integer peso;

    public List<Nodo> getNodos() {
        return nodos;
    }

    public void setNodos(List<Nodo> nodos) {
        this.nodos = nodos;
    }

    public Integer getPeso() {
        return peso;
    }

    public void setPeso(Integer peso) {
        this.peso = peso;
    }
}
