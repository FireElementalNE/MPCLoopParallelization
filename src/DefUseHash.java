import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

class DefUseHash {
    private AssignStmt def;
    private Stmt use;

    DefUseHash(AssignStmt def, Stmt use) {
        this.def = def;
        this.use = use;
    }
}
