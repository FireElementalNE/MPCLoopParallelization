import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;

public class Variable {
    private Map<String, Set<String>> aliases;
    private Value root_val;
    private MutableGraph var_graph;
    Variable(Value v) {
        this.root_val = v;
        this.aliases = new HashMap<>();
        aliases.put(v.toString(), new HashSet<>());
        this.var_graph = null;
    }
    Variable(Variable var) {
        this.aliases = new HashMap<>(var.aliases);
        this.root_val = var.root_val;
        this.var_graph = var.var_graph;
    }

    void add_alias(ImmutablePair<Value, Value> link) {
        String old_val = link.getLeft().toString();
        String new_val = link.getRight().toString();
        // check if it changing an existing value
        for(Map.Entry<String, Set<String>> entry : aliases.entrySet()) {
            if(Objects.equals(entry.getKey(), old_val)) {
                entry.getValue().add(new_val);
                aliases.put(new_val, new HashSet<>());
                break;
            }
        }
    }

    public boolean has_ever_been(Value v) {
        for(Map.Entry<String, Set<String>> entry : aliases.entrySet()) {
            if(Objects.equals(entry.getKey(), v.toString())) {
                return true;
            }
            for(String s : entry.getValue()) {
                if(Objects.equals(s, v.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void parse_node(String current_var) {
        guru.nidi.graphviz.model.Node current_node = node(current_var);
        Set<String> children = aliases.get(current_var);
        if(children.isEmpty()) {
            var_graph.add(current_node);
        } else {
            for (String s : aliases.get(current_var)) {
                guru.nidi.graphviz.model.Node child = node(s);
                var_graph.add(current_node.link(to(child).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
                parse_node(s);
            }
        }
    }

    public void make_graph(String phi_name) {
        String graph_name = String.format("%s_%S", phi_name, root_val.toString());
        this.var_graph = mutGraph(graph_name).setDirected(true);
        parse_node(root_val.toString());
        Utils.print_graph(var_graph);
    }
}
