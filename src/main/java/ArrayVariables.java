import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ArrayVariables {
    private Map<String, ArrayVersion> array_vars;

    ArrayVariables() {
        this.array_vars = new HashMap<>();
    }

    ArrayVariables(String name, ArrayVersion base_ver) {
        array_vars = new HashMap<>();
        array_vars.put(name, base_ver);
    }

    ArrayVariables(ArrayVariables av) {
        this.array_vars = av.array_vars.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    void put(String name, ArrayVersion av) {
        array_vars.put(name, av);
    }

    void remove(String name) {
        array_vars.remove(name);
    }

    ArrayVersion get(String name) {
        return array_vars.getOrDefault(name, Constants.VAR_NOT_FOUND);
    }

    boolean contains_key(String name) {
        return array_vars.containsKey(name);
    }

    Set<Map.Entry<String, ArrayVersion>> entry_set() {
        return array_vars.entrySet();
    }

    boolean has_been_written_to(String name) {
        return array_vars.get(name).has_been_written_to();
    }

    void toggle_written(String name) {
        ArrayVersion av = array_vars.get(name);
        av.toggle_written();
        array_vars.put(name, av);
    }
}
