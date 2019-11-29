import java.util.ArrayList;
import java.util.Vector;

public class PathNode {
    //Object for the recording the traversal of the adjacency matrix

    private Vector<Integer> coordinates;     // x and y pos of this node in the adjacency matrix
    private ArrayList<Vector> pathSoFar;    // vector coordinates travelled to before reaching this coordinate

    public PathNode(int x, int y, ArrayList<Vector> pathSoFar) {
        coordinates = new Vector<>(x, y);
        this.pathSoFar = pathSoFar;
    }

    public Vector<Integer> getVectorCoordinate() {
        return coordinates;
    }

    public int getRow() {
        return coordinates.firstElement();
    }

    public int getCol() {
        return coordinates.elementAt(1);
    }
}
