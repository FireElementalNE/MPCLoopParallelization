import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

class Node {
    private Stmt stmt;
    private DefOrUse type;
    private String base_name;


    Node(Stmt stmt, ArrayRef aref) {
        assert !(stmt instanceof AssignStmt);
        this.stmt = stmt;
        this.type = DefOrUse.USE;
        this.base_name = aref.getBaseBox().getValue().toString();
    }

    Node(AssignStmt stmt, ArrayRef aref) {
        this.stmt = stmt;
        this.type = DefOrUse.DEF;
        this.base_name = aref.getBaseBox().getValue().toString();
    }

    Node(AssignStmt stmt) {
        // phi
        this.stmt = stmt;
        this.type = DefOrUse.DEF;
        this.base_name = stmt.getLeftOpBox().getValue().toString();
    }

    Stmt get_stmt() {
        return stmt;
    }

    String get_id() {
        return base_name + "_" + type.toString();
    }

}
