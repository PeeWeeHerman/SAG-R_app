package ingenieria.de.software.sherly.model;

import java.math.BigInteger;

public class Node {
    private String id;
    private String title;
    private float x;
    private float y;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
