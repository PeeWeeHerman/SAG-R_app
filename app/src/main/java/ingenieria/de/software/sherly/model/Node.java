package ingenieria.de.software.sherly.model;

public class Node {
    private String id;
    private String title;
    private long x;
    private long y;

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

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
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
