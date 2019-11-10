package ingenieria.de.software.sherly.model;

import java.util.List;

public class Map {
    List<Node> nodes;
    List<Edge> edges;

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    @Override
    public String toString() {
        return "Map{" +
                "nodes=" + nodes +
                ", edges=" + edges +
                '}';
    }
}
