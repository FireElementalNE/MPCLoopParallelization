import soot.ValueBox;

import java.util.ArrayList;
import java.util.List;

public class ArraySSAPhi {
    ValueBox base;
    List<String> copy_names;
    int copy_count;
    public ArraySSAPhi(ValueBox base) {
        this.base = base;
        copy_names = new ArrayList<>();
        copy_count = 1;
        copy_names.add(base.getValue().toString() + "_1");
        add_copy();
    }
    public void add_copy() {
        copy_names.add(String.format(Constants.SSA_NAME, base.getValue().toString(), copy_count + 1));
        copy_count++;
    }

    public String get_latest_copy() {
        return copy_names.get(copy_names.size() - 1);
    }

    public String get_prev_copy() {
        assert copy_names.size() > 2;
        return copy_names.get(copy_names.size() - 2);
    }

    public String get_phi_str() {
        return String.format(Constants.SSA_PHI, base.getValue().toString(), get_latest_copy());
    }
}
