import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Variable {
    // TODO: this class is going to have to change, see large comment in PhiVariable.java
    private Map<Integer, ImmutablePair<Value, AssignStmt>> previous_values;
    private PhiExpr phi_expr;
    private Value phi_var;
    private Value current_value;
    private AssignStmt current_assignment;
    private int counter;

    Variable(Value v, AssignStmt s, Value phi_var, PhiExpr phi_expr) {
        this.current_value = v;
        this.phi_expr = phi_expr;
        this.current_assignment = s;
        this.phi_var = phi_var;
        this.counter = 1;
        this.previous_values = new HashMap<>();
    }

    Variable(Variable v) {
        this.current_value = v.current_value;
        this.current_assignment = v.current_assignment;
        this.previous_values = v.previous_values.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.counter = v.counter;
        this.phi_expr = v.phi_expr;
        this.phi_var = v.phi_var;
    }

    @SuppressWarnings("unused")
    boolean has_ever_been(Value v) {
        // must be int!
        assert Objects.equals(v.getType().toString(), Constants.INT_TYPE);
        if(Objects.equals(current_value.toString(), v.toString())) {
            return true;
        } else {
            for(Map.Entry<Integer, ImmutablePair<Value, AssignStmt>> entry : previous_values.entrySet()) {
                ImmutablePair <Value, AssignStmt> pair = entry.getValue();
                if(Objects.equals(pair.getLeft().toString(), v.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

//    void make_graph() {
//        String graph_name = String.format("%s%d", current_version, counter);
//        MutableGraph var_graph = mutGraph(graph_name).setDirected(true);
//        String prev_name = String.format("%s", current_version);
//        guru.nidi.graphviz.model.Node prev_node = node(prev_name);
//        for(int i = 1; i < previous_versions.size() + 1; i++) {
//            if(i != 1) {
//                assert previous_versions.containsKey(i - 1);
//                prev_name = String.format("%s",  previous_versions.get(i - 1).getLeft());
//                prev_node = node(prev_name);
//            }
//            assert previous_versions.containsKey(i);
//            String current_name =  previous_versions.get(i).getLeft();
//            guru.nidi.graphviz.model.Node current_node = node(current_name);
//            var_graph.add(prev_node.link(to(current_node).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
//        }
//        try {
//            Graphviz.fromGraph(var_graph).width(Constants.GRAPHVIZ_WIDTH).render(Format.PNG)
//                    .toFile(new File(Utils.make_graph_name(var_graph.name().toString())));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    Value get_current_version() {
        return (Value) current_value.clone();
    }

    void add_alias(Value v, AssignStmt s) {
        ImmutablePair <Value, AssignStmt> pair = new ImmutablePair<>(current_value, current_assignment);
        previous_values.put(counter, pair);
        counter++;
        current_value = (Value) v.clone();
        current_assignment = (AssignStmt) s.clone();
    }

}
