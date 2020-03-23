import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: possibly rename this?
public class PhiVariableContainer {

    private Set<PhiVariable> phi_vars;

    /**
     * create a PhiVariableContainer
     * this is a convenience class to cut down on unreadable code. It contains all of the phi variables, as well
     * as their associated functions
     */
    PhiVariableContainer() {
        this.phi_vars = new HashSet<>();
    }

    /**
     * Copy constructor for the PhiVariableContainer class
     * @param pvc the PhiVariable container being copied
     */
    PhiVariableContainer(PhiVariableContainer pvc) {
        this.phi_vars = pvc.phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
    }

    /**
     * add a new phi variable.
     * @param pv the PhiVariable to add
     */
    void add(PhiVariable pv) {
        phi_vars.add(new PhiVariable(pv));
    }

    /**
     * process an assignment statement, finding:
     * 1. new looping statements
     * 2. new aliases
     * @param stmt the assignment being analyzed
     */
    void process_assignment(AssignStmt stmt) {
        ValueBox left = stmt.getLeftOpBox();
        for(PhiVariable pv : phi_vars) {
            List<ImmutablePair<Value, Value>> values = pv.get_phi_var_uses(stmt);
            // if we are REDEFINING a phi variable it is a looping stmt.
            if(pv.defines_phi_var(stmt) && !values.isEmpty()) {
                Logger.debug("Found that stmt '" + stmt.toString()  + "' links to phi stmt '" + pv.toString() + "'.");
                Logger.debug("This is most likely a Looping stmt.");
                pv.add_linked_stmt(stmt);
            }
            else if(!values.isEmpty() && !(left.getValue() instanceof ArrayRef)) {
                // if we we are not DEFINING a phi var but we are using one
                Logger.debug("Found that stmt '" + stmt.toString() + "' uses phi vars:");
                Logger.debug("\toriginal phi: " + pv.toString());
                for(ImmutablePair<Value, Value> v_pair : values) {
                    Logger.debug("\t  " + v_pair.getLeft().toString() + " is effected by " + v_pair.getRight().toString());
                }
                pv.add_alias(left, stmt, values);
            }
        }
    }

    /**
     * make graphs for all of the PhiVariables
     */
    void make_graphs() {
        for(PhiVariable pv : phi_vars) {
            pv.make_graph();
        }
    }

    /**
     * print a def/use string for a given variable
     * @param v the variable name
     */
    void print_var_dep_chain(String v) {
        // TODO: this is a proof of concept function
        for(PhiVariable pv : phi_vars) {
            if(pv.has_ever_been(v)) {
                String s = pv.get_var_dep_chain(v);
                if(Utils.not_null(s)) {
                    // make sure it is not null!
                    Logger.info(s);
                }
            }
        }
    }
}