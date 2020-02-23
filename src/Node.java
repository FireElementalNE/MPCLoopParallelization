import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

class Node {
    private Stmt stmt;
    private DefOrUse type;
    private ArraySSAPhi phi;

    Node(Stmt stmt) {
        this.stmt = stmt;
        this.type = DefOrUse.USE;
        this.phi = null;
    }

    Node(AssignStmt stmt) {
        this.stmt = stmt;
        this.type = DefOrUse.DEF;
    }

    void set_phi(ArraySSAPhi phi) {
        this.phi = phi;
    }

    Stmt get_stmt() {
        return stmt;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(stmt);
    }

}
