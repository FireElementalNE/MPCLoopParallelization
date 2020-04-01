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
    private Index index;
    private String basename;
    private String aug_stmt;
    private boolean is_aug;
    private boolean phi_flag;
    private int line_num;


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
    }

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
    }

    Node(String basename, ArrayVersion av) {
        // phi
        this.stmt = Utils.create_phi_stmt(basename, av);
        this.type = DefOrUse.DEF;
        this.av = av;
        this.basename = basename;
        this.phi_flag = av.is_phi();
        this.index = new Index();
        this.line_num = Constants.PHI_LINE_NUM;
    }

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
