import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

import java.util.Objects;

class Node {
    private String stmt;
    private DefOrUse type;
    private ArrayVersion av;
    private String basename;


    Node(String stmt, String basename, ArrayVersion av, DefOrUse type) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.basename = basename;
    }

    Node(String basename, ArrayVersion av) {
        // phi
        this.stmt = Utils.create_phi_stmt(basename, av);
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

//    void process_redefine(Stmt stmt, String basename, ArrayVersion av) {
//        this.basename = basename;
//        this.av = new ArrayVersion(av);
//        this.stmt = stmt;
//    }

    String get_stmt() {
        return stmt;
    }

    ArrayVersion get_av() {
        return av;
    }

    @SuppressWarnings("DuplicatedCode")
    String get_id() {
        if(av.is_phi()) {
            ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
            if (av.has_diff_ver_match()) {
                return basename + "_" + av_phi.get_av1().get_version() + "a_" + av_phi.get_av2().get_version() + "b_" + type;
            }
            return basename + "_" + av_phi.get_av1().get_version() + "_" + av_phi.get_av2().get_version() + "_" + type;
        }
        ArrayVersionSingle av_single = (ArrayVersionSingle)av;
        return basename + "_" + av_single.get_version() + "_" + type;
    }

    @SuppressWarnings("DuplicatedCode")
    String get_opposite_id() {
        DefOrUse t = Objects.equals(DefOrUse.DEF, type) ? DefOrUse.USE : DefOrUse.DEF;
        if(av.is_phi()) {
            ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
            if (av.has_diff_ver_match()) {
                return basename + "_" + av_phi.get_av1().get_version() + "a_" + av_phi.get_av2().get_version() + "b_" + t;
            }
            return basename + "_" + av_phi.get_av1().get_version() + "_" + av_phi.get_av2().get_version() + "_" + t;
        }
        ArrayVersionSingle av_single = (ArrayVersionSingle)av;
        return basename + "_" + av_single.get_version() + "_" + t;
    }

    @SuppressWarnings("DuplicatedCode")
    static String make_id(String id, ArrayVersion av, DefOrUse t) {
        if(av.is_phi()) {
            ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
            if (av.has_diff_ver_match()) {
                return id + "_" + av_phi.get_av1().get_version() + "a_" + av_phi.get_av2().get_version() + "b_" + t;
            }
            return id + "_" + av_phi.get_av1().get_version() + "_" + av_phi.get_av2().get_version() + "_" + t;
        }
        ArrayVersionSingle av_single = (ArrayVersionSingle)av;
        return id + "_" + av_single.get_version() + "_" + t;
    }
}
