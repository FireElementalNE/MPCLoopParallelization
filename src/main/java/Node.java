import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

import java.util.Objects;

class Node {
    private Stmt stmt;
    private DefOrUse type;
    private ArrayVersion av;
    private String basename;


    Node(Stmt stmt, String basename, ArrayVersion av, DefOrUse type) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.basename = basename;
    }

    Node(String basename, ArrayVersion av) {
        // phi
        this.stmt = null;
        this.type = DefOrUse.DEF;
        this.av = av;
        this.basename = basename;
    }

    Node(Node n) {
        this.stmt = n.stmt;
        this.type = n.type;
        this.av = n.av;
        this.basename = n.basename;
    }

    void process_redefine(Stmt stmt, String basename, ArrayVersion av) {
        this.basename = basename;
        this.av = new ArrayVersion(av);
        this.stmt = stmt;
    }

    Stmt get_stmt() {
        return stmt;
    }

    ArrayVersion get_av() {
        return av;
    }

    @SuppressWarnings("DuplicatedCode")
    String get_id() {
        if(av.is_phi()) {
            if (av.has_diff_ver_match()) {
                return basename + "_" + av.get_v1() + "a_" + av.get_v2() + "b_" + type;
            }
            return basename + "_" + av.get_v1() + "_" + av.get_v2() + "_" + type;
        }
        return basename + "_" + av.get_v1() + "_" + type;
    }

    @SuppressWarnings("DuplicatedCode")
    String get_opposite_id() {
        DefOrUse t = Objects.equals(DefOrUse.DEF, type) ? DefOrUse.USE : DefOrUse.DEF;
        if(av.is_phi()) {
            if (av.has_diff_ver_match()) {
                return basename + "_" + av.get_v1() + "a_" + av.get_v2() + "b_" + t;
            }
            return basename + "_" + av.get_v1() + "_" + av.get_v2() + "_" + t;
        }
        return basename + "_" + av.get_v1() + "_" + t;
    }

    @SuppressWarnings("DuplicatedCode")
    static String make_id(String id, ArrayVersion av, DefOrUse t) {
        if(av.is_phi()) {
            if (av.has_diff_ver_match()) {
                return id + "_" + av.get_v1() + "a_" + av.get_v2() + "b_" + t;
            }
            return id + "_" + av.get_v1() + "_" + av.get_v2() + "_" + t;
        }
        return id + "_" + av.get_v1() + "_" + t;
    }
}
