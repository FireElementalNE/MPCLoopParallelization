import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import soot.jimple.Stmt;

import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

@SuppressWarnings("FieldMayBeFinal")
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
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Utils.copy_av(entry.getValue())));
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

    void make_array_var_graph(Set<String> constants) {
        // TODO: this does not contain all the info!
        for(Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
            MutableGraph ver_graph = mutGraph(entry.getKey() + "_versions").setDirected(true);
            Map<Integer, Stmt> versions_map = entry.getValue().get_versions();
            List<Integer> versions = new ArrayList<>(versions_map.keySet());
            Collections.sort(versions);
            int v = versions.get(0);
            guru.nidi.graphviz.model.Node cur_node = node(v + ": " + versions_map.get(v).toString());
            for(int i = 1; i < versions.size(); i++) {
                v = versions.get(i);
                guru.nidi.graphviz.model.Node new_node = node(v + ": " + versions_map.get(v).toString());
                ver_graph.add(cur_node.link(to(new_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                cur_node = new_node;
            }
            Utils.print_graph(ver_graph);
        }
    }
}
