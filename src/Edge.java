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

    void set_def_phi(ArraySSAPhi phi) {
        def.set_phi(phi);
    }

    void set_use_phi(ArraySSAPhi phi) {
        use.set_phi(phi);
    }

    @Override
    public int hashCode() {
        assert def.get_stmt() instanceof AssignStmt;
        return System.identityHashCode(new DefUseHash((AssignStmt)def.get_stmt(), use.get_stmt()));
    }
}
