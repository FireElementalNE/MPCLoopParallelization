import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Value;
import soot.jimple.AssignStmt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Variable {
    private Map<Integer, ImmutablePair<Value, AssignStmt>> previous_versions;
    private Value current_version;
    private AssignStmt current_stmt;
    private int counter;

    Variable(Value v, AssignStmt s) {
        this.current_version = v;
        this.current_stmt = s;
        this.counter = 1;
        this.previous_versions = new HashMap<>();
    }

    Variable(Variable v) {
        this.current_version = v.current_version;
        this.current_stmt = v.current_stmt;
        this.previous_versions = v.previous_versions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.counter = v.counter;
    }

    boolean has_ever_been(Value v) {
        // must be int!
        assert Objects.equals(v.getType().toString(), Constants.INT_TYPE);
        if(Objects.equals(current_version.toString(), v.toString())) {
            return true;
        } else {
            for(Map.Entry<Integer, ImmutablePair<Value, AssignStmt>> entry : previous_versions.entrySet()) {
                ImmutablePair <Value, AssignStmt> pair = entry.getValue();
                if(Objects.equals(pair.getLeft().toString(), v.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    Value get_current_version() {
        return current_version;
    }

    void add_alias(Value v, AssignStmt s) {
        ImmutablePair <Value, AssignStmt> pair = new ImmutablePair<>(current_version, current_stmt);
        previous_versions.put(counter, pair);
        counter++;
        current_version = v;
        current_stmt = s;
    }

}
