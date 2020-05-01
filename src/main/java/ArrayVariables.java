import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import soot.jimple.Stmt;

import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * class to store all actual array variables
 */
@SuppressWarnings("FieldMayBeFinal")
public class ArrayVariables {
    private Map<String, ArrayVersion> array_vars;

    /**
     * blank constructor for array variable class
     */
    ArrayVariables() {
        this.array_vars = new HashMap<>();
    }

    /**
     * constructor for array variables with a first entry passed
     * @param name the name of the new entry
     * @param base_ver the base of the new entry
     */
    ArrayVariables(String name, ArrayVersion base_ver) {
        array_vars = new HashMap<>();
        array_vars.put(name, base_ver);
    }

    /**
     * copy constructor for the class
     * @param av the other ArrayVariables object
     */
    ArrayVariables(ArrayVariables av) {
        this.array_vars = av.array_vars.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Utils.copy_av(entry.getValue())));
    }

    /**
     * place a new entry into the variable map
     * @param name the name of the entry
     * @param av the version of the entry
     */
    void put(String name, ArrayVersion av) {
        array_vars.put(name, av);
    }

    /**
     * remover an entry from the variable map
     * @param name the name of the entry
     */
    void remove(String name) {
        array_vars.remove(name);
    }

    /**
     * the the version for a member
     * @param name the name of the member
     * @return the version of the member if it exists or VAR_NOT_FOUND from Constants.java
     */
    ArrayVersion get(String name) {
        return array_vars.getOrDefault(name, Constants.VAR_NOT_FOUND);
    }

    /**
     * test if the map has a certain name as a key
     * @param name the name to test
     * @return true iff the name is in the KeySet of the variable map
     */
    boolean contains_key(String name) {
        return array_vars.containsKey(name);
    }

    /**
     * get the entry set of the variable map
     * @return the entry set for the variable map
     */
    Set<Map.Entry<String, ArrayVersion>> entry_set() {
        return array_vars.entrySet();
    }

    /**
     * return entry at the passed name has been written to
     * @param name the name of the entry to test
     * @return true iff the entry at the passed name in the map has been written to
     */
    boolean has_been_written_to(String name) {
        return array_vars.get(name).has_been_written_to();
    }

    /**
     * change the written flag on an entry
     * @param name the name of the entry
     */
    void toggle_written(String name) {
        ArrayVersion av = array_vars.get(name);
        av.toggle_written();
        array_vars.put(name, av);
    }

    /**
     * make a graph representing all the versions of this variable
     */
    void make_array_var_graph() {
        // TODO: this does not contain all the info!
        for(Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
            MutableGraph ver_graph = mutGraph(entry.getKey() + "_versions").setDirected(true);
            Map<Integer, Stmt> versions_map = entry.getValue().get_versions();
            List<Integer> versions = new ArrayList<>(versions_map.keySet());
            Collections.sort(versions);
            int v = versions.get(0);
            guru.nidi.graphviz.model.Node cur_node = node(v + ": " + versions_map.get(v).toString());
            if(versions.size() > 1) {
                for (int i = 1; i < versions.size(); i++) {
                    v = versions.get(i);
                    guru.nidi.graphviz.model.Node new_node = node(v + ": " + versions_map.get(v).toString());
                    ver_graph.add(cur_node.link(to(new_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                    cur_node = new_node;
                }
            } else {
                ver_graph.add(cur_node);
            }
            Utils.print_graph(ver_graph);
        }
    }
}
