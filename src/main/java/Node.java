import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An _overly_ complex node class for ArrayDefUseGraph
 */
class Node {

    private String stmt; // the statement the node represents
    private DefOrUse type; // type of node (definition or usage)
    private ArrayVersion av; // ArrayVersion keeps track of the array version represented in the node
    private Index index; // the index of the array reference
    private String basename; // the basename of the array reference
    private String aug_stmt; // the augmented statement based on any new definitions
    private boolean is_aug; // this node has been augmented
    private boolean phi_flag; // this is a phi node (special)
    private boolean is_prev_loop_dummy; // this node is a dummmy node representing the entire previous iteration
    private int line_num; // the line number of the statement

    /**
     * Constructor for a brand NEW node. This will either have ArrayVersions of -1 (as a dummy node
     * referencing a previous iteration) or 1.
     * @param stmt the statement that the node represents (As a string)
     * @param basename the basename ofr hte array reference
     * @param av the NEW array version
     * @param index the index of the array reference
     * @param type the type of node (definition of usage)
     * @param line_num the line number of the statement
     */
    Node(String stmt, String basename, ArrayVersion av, Index index, DefOrUse type, int line_num) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.phi_flag = av.is_phi();
        this.basename = basename;
        this.aug_stmt = null;
        this.is_aug = false;
        this.index = index;
        this.line_num = line_num;
        this.is_prev_loop_dummy = false;
    }

    /**
     * Constructor for a node that is being copied (and possibly renamed) from an old node.
     * @param stmt the statement that the node represents (As a string)
     * @param basename the basename ofr hte array reference
     * @param av the array version that the old node had
     * @param index the index of the array reference
     * @param type the type of node (definition of usage)
     * @param replacements a pair of strings that represents the old augemented name of the node (based off of the array
     *                     OLD array version) and the new augemented name of the node. This is used to change the
     *                     aug_stmt
     * @param line_num the line number of the statement
     */
    Node(String stmt, String basename, ArrayVersion av, Index index, DefOrUse type, ImmutablePair<String, String> replacements, int line_num) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.phi_flag = av.is_phi();
        this.index = index;
        this.basename = basename;
        this.is_aug = true;
        this.aug_stmt = stmt.replace(replacements.getLeft(), replacements.getRight());
        this.line_num = line_num;
        this.is_prev_loop_dummy = false;
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
        this.line_num = Constants.PHI_LINE_NUM;
        this.is_prev_loop_dummy = false;
    }

    /**
     * Constructor for a dummy node that represents the entire previous iterations. No important info is
     * held in it.
     * @param stmt the statement that the node represents (Is always static)
     * @param basename the basename of the array reference
     */
    Node(String stmt, String basename) {
        this.stmt = stmt;
        this.basename = basename;
        this.is_prev_loop_dummy = true;
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
        this.line_num = n.line_num;
        this.is_prev_loop_dummy = n.is_prev_loop_dummy;
    }

    String get_stmt() {
        if(is_aug) {
            return aug_stmt;
        } else {
            return stmt;
        }
    }

    String get_opposite_id() {
        DefOrUse t = Objects.equals(DefOrUse.DEF, type) ? DefOrUse.USE : DefOrUse.DEF;
        if(t == DefOrUse.DEF) {
            return Node.make_id(basename, av, t, line_num);
        } else {
            return Node.make_id(basename, av, t, line_num);
        }

    }

    static String make_id(String id, ArrayVersion av, DefOrUse t, int line_num) {
        StringBuilder sb = new StringBuilder(id);
        sb.append(Constants.UNDERSCORE);
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
            sb.append(t);
        } else {
            ArrayVersionSingle av_single = (ArrayVersionSingle) av;
            sb.append(av_single.get_version());
            sb.append(Constants.UNDERSCORE);
            sb.append(t);
        }
        if(t == DefOrUse.USE) {
            sb.append(":");
            sb.append(line_num);
        }
        return sb.toString();
    }

    ArrayVersion get_av() {
        return av;
    }

    boolean is_aug() {
        return is_aug;
    }

    boolean is_phi() {
        return phi_flag;
    }

    Index get_index() {
        return index;
    }

    String get_id() {
        return Node.make_id(basename, av, type, line_num);
    }

    int get_line_num() {
        return line_num;
    }
}
