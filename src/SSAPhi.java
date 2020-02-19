import soot.ValueBox;

import java.util.ArrayList;
import java.util.List;

public class SSAPhi {
    ValueBox base;
    List<String> copy_names;
    int copy_count;
    public SSAPhi(ValueBox base) {
        this.base = base;
        copy_names = new ArrayList<>();
        copy_count = 1;
    }
    public String add_copy() {
        String new_name = base.getValue().toString() + "_" + copy_count;
        copy_names.add(new_name);
        copy_count++;
        return new_name;
    }

    public String get_phi_str() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < copy_names.size(); i++) {
            sb.append(String.format(Constants.SSA_PHI, base.getValue().toString(), copy_names.get(i)));
            if(i + 1 < copy_names.size()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
