import org.pmw.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ArraySSAPhi {
    private Map<String, AssignStmt> definitions;
    private ValueBox base;
    private List<String> copy_names;
    private int copy_count;
    ArraySSAPhi(ValueBox base, AssignStmt stmt) {
        definitions = new HashMap<>();
        this.base = base;
        copy_names = new ArrayList<>();
        copy_count = 1;
        String first_copy = this.base.getValue().toString() + "_1";
        copy_names.add(first_copy);
        add_copy(stmt);
    }
    void add_copy(AssignStmt stmt) {
        String new_name = String.format(Constants.SSA_NAME, base.getValue().toString(), copy_count + 1);
        copy_names.add(new_name);
        definitions.put(new_name, stmt);
        copy_count++;
    }

    AssignStmt get_latest_definition() {
        if(definitions.containsKey(copy_names.get(copy_names.size() - 1))) {
            return definitions.get(copy_names.get(copy_names.size() - 1));
        }
        Logger.error("Key '" + copy_names.get(copy_names.size() - 1) + "' not found in definition map");
        return null;
    }

    String get_latest_name() {
        return copy_names.get(copy_names.size() - 1);
    }

    String get_prev_copy() {
        assert copy_names.size() > 2;
        return copy_names.get(copy_names.size() - 2);
    }

    String get_phi_str() {
        return String.format(Constants.SSA_PHI, base.getValue().toString(), get_latest_name());
    }

    String get_base_name() {
        return base.toString();
    }

}
