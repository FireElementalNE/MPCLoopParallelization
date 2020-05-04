import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An _overly_ complex node class for ArrayDefUseGraph
 */
@SuppressWarnings("unused")
class Node {
    /**
     * the statement the node represents
     */
    private final String stmt;
    /**
     * type of node (definition or usage)
     */
    private final DefOrUse type;
    /**
     * ArrayVersion keeps track of version of the array  represented in the node
     */
    private ArrayVersion av;
    /**
     * the index of the array reference
     */
    private final Index index;
    /**
     * the basename of the array reference
     */
    private final String basename;
    /**
     * the augmented statement based on any new definitions
     */
    private String aug_stmt;
    /**
     * flag showing if this node is augmented
     */
    private boolean is_aug;
    /**
     * this is a phi node (special)
     */
    private final boolean phi_flag;
    /**
     * flag showing if this node is a base (first) definition
     */
    private final boolean base_def;

    /**
     * Constructor for a brand NEW node. This will either have ArrayVersions of -1 (as a dummy node
     * referencing a previous iteration) or 1.
     * @param stmt the statement that the node represents (As a string)
     * @param basename the basename ofr hte array reference
     * @param av the NEW array version
     * @param index the index of the array reference
     * @param type the type of node (definition of usage)
     * @param base_def true iff we are dealing with a base definition or a pure rename, these do NOT need
     *                 to be included in any edges!
     */
    Node(String stmt, String basename, ArrayVersion av, Index index,
         DefOrUse type, boolean base_def) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.phi_flag = av.is_phi();
        this.basename = basename;
        this.aug_stmt = null;
        this.is_aug = false;
        this.index = index;
        this.base_def = base_def;
    }

    /**
     * Constructor for a node that is being copied (and possibly renamed) from an old node.
     * @param stmt the statement that the node represents (As a string)
     * @param basename the basename ofr hte array reference
     * @param av the array version that the old node had
     * @param index the index of the array reference
     * @param type the type of node (definition of usage)
     * @param replacements a pair of strings that represents the old augmented name of the node (based off of the array
     *                     OLD array version) and the new augmented name of the node. This is used to change the
     *                     aug_stmt
     * @param base_def true iff we are dealing with a base definition or a pure rename, these do NOT need
     *                 to be included in any edges!
     */
    Node(String stmt, String basename, ArrayVersion av, Index index,
         DefOrUse type, ImmutablePair<String, String> replacements, boolean base_def) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.phi_flag = av.is_phi();
        this.index = index;
        this.basename = basename;
        this.is_aug = true;
        this.aug_stmt = stmt.replace(replacements.getLeft(), replacements.getRight());
        this.base_def = base_def;
    }

    /**
     * Constructor for a node if it is a Phi node (for arrays!!)
     * @param basename the basename of the array
     * @param av the array version
     */
    Node(String basename, ArrayVersion av) {
        // phi
        this.stmt = Utils.create_phi_stmt(basename, av);
        this.type = DefOrUse.DEF;
        this.av = av;
        this.basename = basename;
        this.phi_flag = av.is_phi();
        this.index = new Index();
        this.base_def = false;
    }

    /**
     * Node Copy constructor (prevent shallow copies as much as possible!)
     * @param n the Node being copied
     */
    Node(Node n) {
        this.stmt = n.stmt;
        this.type = n.type;
        this.av = n.av;
        this.basename = n.basename;
        this.is_aug = n.is_aug;
        this.aug_stmt = n.aug_stmt;
        this.index = n.index;
        this.phi_flag = n.phi_flag;
        this.base_def = n.base_def;
    }

//    String resolve_index(PhiVariableContainer pvc, Map<String, Integer> constants) {
//        if(!is_phi()) {
//            String index_str = index.to_str();
//            if(!Objects.equals(index_str, Constants.ARRAY_VERSION_NEW_ARRAY)) {
//                ImmutablePair<Variable, List<AssignStmt>> dep_chain = pvc.get_var_dep_chain(constants, index_str);
//                String eq;
//                if(Utils.not_null(dep_chain)) {
//                    Solver def_solver = new Solver(index_str, dep_chain, pvc, constants);
//                    eq = def_solver.get_resolved_eq();
//                    if (eq.contains("=")) {
//                        eq = eq.split(" = ")[1];
//                    }
//                } else {
//                    eq = "CREATED_ARRAY_SSA_PHI_NODE";
//                }
//                return eq;
//            } else {
//                return Constants.CANNOT_RESOLVE_NODE_EQ;
//            }
//        } else {
//            return Constants.CANNOT_RESOLVE_NODE_EQ;
//        }
//    }
//
//    int resolve_d(PhiVariableContainer pvc, Map<String, Integer> constants) {
//        if(!is_phi()) {
//            String index_str = index.to_str();
//            if(!Objects.equals(index_str, Constants.ARRAY_VERSION_NEW_ARRAY)) {
//                ImmutablePair<Variable, List<AssignStmt>> use_dep_chain = pvc.get_var_dep_chain(constants, index_str);
//                Solver use_solver = new Solver(index_str, use_dep_chain, pvc, constants);
//                return use_solver.solve();
//            } else {
//                return Constants.CANNOT_RESOLVE_NODE_D;
//            }
//        } else {
//            return Constants.CANNOT_RESOLVE_NODE_D;
//        }
//    }


    /**
     * get the original node stmt
     * @return the possibly augmented node statement
     */
    String get_stmt() {
        return stmt;
    }

    /**
     * get the node statement (might be augmented)
     * @return the possibly augmented node statement
     */
    String get_aug_stmt() {
        if(is_aug) {
            return aug_stmt;
        } else {
            return stmt;
        }
    }

    /**
     * get the opposite id of the node (if DEF then USE and vice versa)
     * @return the opposite id
     */
    String get_opposite_id() {
        DefOrUse t = Objects.equals(DefOrUse.DEF, type) ? DefOrUse.USE : DefOrUse.DEF;
        return Node.make_id(basename, av, t);

    }

    /**
     * make an id for a node
     * @param id the id of the node
     * @param av the array version of the node
     * @param t the type of the node
     * @return a node id as a string
     */
    static String make_id(String id, ArrayVersion av, DefOrUse t) {
        StringBuilder sb = new StringBuilder(id);
        sb.append(Constants.UNDERSCORE);
        int line_num = av.get_line_num();
        if(av.is_phi()) {
            ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
            Map<Integer, Integer> have_seen = new HashMap<>();
            List<Integer> versions = av_phi.get_array_versions().stream()
                    .map(ArrayVersion::get_version)
                    .collect(Collectors.toList());
            for(int s : versions) {
                sb.append(s);
                if(av_phi.has_diff_ver_match()) {
                    if(have_seen.containsKey(s)) {
                        have_seen.put(s, have_seen.get(s) + 1);
                    } else {
                        have_seen.put(s, 0);
                    }
                    sb.append(Constants.ALPHABET_ARRAY[have_seen.get(s)]);
                }
                sb.append(Constants.UNDERSCORE);
            }
        } else {
            ArrayVersionSingle av_single = (ArrayVersionSingle) av;
            sb.append(av_single.get_version());
            sb.append(Constants.UNDERSCORE);
        }
        sb.append(t);
        if(t == DefOrUse.USE) {
            sb.append(":");
            sb.append(line_num);
        }
        return sb.toString();
    }

    /**
     * getter for array version
     * @return the array version of the node
     */
    ArrayVersion get_av() {
        return av;
    }

    /**
     * the getter for the augmented flag
     * @return the augmented flag
     */
    boolean is_aug() {
        return is_aug;
    }

    /**
     * the getter for the phi flag
     * @return the phi flag
     */
    boolean is_phi() {
        return phi_flag;
    }

    /**
     * the getter for the index
     * @return the index
     */
    Index get_index() {
        return index;
    }

    /**
     * get the ID of this node
     * @return  the ID of this node
     */
    String get_id() {
        return Node.make_id(basename, av, type);
    }

    /**
     * getter for the line number of this statement
     * @return the line number for this statement
     */
    int get_line_num() {
        return av.get_line_num();
    }

    /**
     * get the type of node (def or use)
     * @return the type of the node
     */
    DefOrUse get_type() {
        return type;
    }

    /***
     * get the base array name
     * @return the base name of the array
     */
    String get_basename() {
        return basename;
    }

    /**
     * return true iff this node is a  base definition or a pure rename, these do NOT need
     * to be included in any edges!
     * @return base def flag
     */
    boolean is_base_def() {
        return base_def;
    }

    /** if we are trying to place a node with the same name in the ArrayDef use graph,
     *  a unique id is needed. To make that id the AV is artificially increased.
     */
    void force_av_incr() {
        av.force_incr_version();
    }

}
