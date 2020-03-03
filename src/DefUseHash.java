import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

class DefUseHash {
    private String def;
    private String use;

    DefUseHash(String def, String use) {
        this.def = def;
        this.use = use;
    }
}
