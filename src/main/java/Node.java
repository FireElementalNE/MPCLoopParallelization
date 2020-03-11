import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class Node {
    private String stmt;
    private DefOrUse type;
    private ArrayVersion av;
    private String basename;
    private String aug_stmt;
    private boolean is_aug;


    Node(String stmt, String basename, ArrayVersion av, DefOrUse type) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.basename = basename;
        this.aug_stmt = null;
        this.is_aug = false;
    }

    Node(String stmt, String basename, ArrayVersion av, DefOrUse type, ImmutablePair<String, String> replacements) {
        this.stmt = stmt;
        this.type = type;
        this.av = av;
        this.basename = basename;
        this.is_aug = true;
        this.aug_stmt = stmt.replace(replacements.getLeft(), replacements.getRight());
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
        this.is_aug = n.is_aug;
        this.aug_stmt = n.aug_stmt;
    }

//    void process_redefine(Stmt stmt, String basename, ArrayVersion av) {
//        this.basename = basename;
//        this.av = new ArrayVersion(av);
//        this.stmt = stmt;
//    }

    String get_stmt() {
        if(is_aug) {
            return aug_stmt;
        } else {
            return stmt;
        }
    }

    ArrayVersion get_av() {
        return av;
    }

    boolean is_aug() {
        return is_aug;
    }

    String get_id() {
        return Node.make_id(basename, av, type);
    }

    String get_opposite_id() {
        DefOrUse t = Objects.equals(DefOrUse.DEF, type) ? DefOrUse.USE : DefOrUse.DEF;
        return Node.make_id(basename, av, t);
    }

    static String make_id(String id, ArrayVersion av, DefOrUse t) {
        if(av.is_phi()) {
            ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
            StringBuilder sb = new StringBuilder(id);
            sb.append(Constants.UNDERSCORE);
            Map<Integer, Integer> have_seen = new HashMap<>();
            List<Integer> versions = av_phi.get_array_versions().stream()
                    .map(ArrayVersion::get_version)
                    .collect(Collectors.toList());
            for(int s : versions) {
                sb.append(s);
                if(av_phi.has_diff_ver_match()) {
                    if(have_seen.containsKey(s)) {
                        sb.append(Constants.ALPHABET_ARRAY[have_seen.get(s)]);
                        have_seen.put(s, have_seen.get(s) + 1);
                    } else {
                        have_seen.put(s, 0);
                    }
                }
                sb.append(Constants.UNDERSCORE);
            }
            sb.append(t);
            return sb.toString();
        }
        ArrayVersionSingle av_single = (ArrayVersionSingle)av;
        return id + "_" + av_single.get_version() + "_" + t;
    }
}
