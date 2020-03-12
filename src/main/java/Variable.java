import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Value;
import soot.jimple.AssignStmt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

public class Variable {
    private Map<Integer, ImmutablePair<String, String>> previous_versions;
    private String current_version;
    private String current_stmt;
    private int counter;

    Variable(Value v, AssignStmt s) {
        this.current_version = v.toString();
        this.current_stmt = s.toString();
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

    @SuppressWarnings("unused")
    boolean has_ever_been(Value v) {
        // must be int!
        assert Objects.equals(v.getType().toString(), Constants.INT_TYPE);
        if(Objects.equals(current_version, v.toString())) {
            return true;
        } else {
            for(Map.Entry<Integer, ImmutablePair<String, String>> entry : previous_versions.entrySet()) {
                ImmutablePair <String, String> pair = entry.getValue();
                if(Objects.equals(pair.getLeft(), v.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    void make_graph() {
        String graph_name = String.format("%s%d", current_version, counter);
        MutableGraph var_graph = mutGraph(graph_name).setDirected(true);
        String prev_name = String.format("%s", current_version);
        guru.nidi.graphviz.model.Node prev_node = node(prev_name);
        for(int i = 1; i < previous_versions.size() + 1; i++) {
            if(i != 1) {
                assert previous_versions.containsKey(i - 1);
                prev_name = String.format("%s",  previous_versions.get(i - 1).getLeft());
                prev_node = node(prev_name);
            }
            assert previous_versions.containsKey(i);
            String current_name =  previous_versions.get(i).getLeft();
            guru.nidi.graphviz.model.Node current_node = node(current_name);
            var_graph.add(prev_node.link(to(current_node).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
        }
        try {
            Graphviz.fromGraph(var_graph).width(Constants.GRAPHVIZ_WIDTH).render(Format.PNG)
                    .toFile(new File(Utils.make_graph_name(var_graph.name().toString())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String get_current_version() {
        return current_version;
    }

    void add_alias(Value v, AssignStmt s) {
        ImmutablePair <String, String> pair = new ImmutablePair<>(current_version, current_stmt);
        previous_versions.put(counter, pair);
        counter++;
        current_version = v.toString();
        current_stmt = s.toString();
    }

}
