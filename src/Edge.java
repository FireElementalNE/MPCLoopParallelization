import soot.jimple.AssignStmt;

class Edge {
    private Node def;
    private Node use;

    Edge(Node node1, Node node2) {
        this.def = node1;
        this.use = node2;
    }

    Node get_def() {
        return def;
    }

    Node get_use() {
        return use;
    }


    @Override
    public int hashCode() {
        return System.identityHashCode(new DefUseHash(def.get_stmt(), use.get_stmt()));
    }
}
