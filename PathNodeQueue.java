import java.util.LinkedList;

class PathNodeQueue extends LinkedList<PathNode> {

    public PathNode deque() {
        if (size() != 0) {
            PathNode p = getFirst();
            removeFirst();

            return p;
        } else return null;
    }

    public void enque(PathNode p) {
        addLast(p);
    }
}