import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A  convenience class to cut down on unreadable code. It contains all of the phi variables, as well
 * as their associated functions
 */
public class PhiVariableContainer {

    private Set<PhiVariable> phi_vars;

    /**
     * Constructor for PhiVariableContainer
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
     * @return true iff a link was found
     */
    boolean process_assignment(AssignStmt stmt) {
        boolean found_link = false;
        ValueBox left = stmt.getLeftOpBox();
        for(PhiVariable pv : phi_vars) {
            List<ImmutablePair<Value, Value>> values = pv.get_phi_var_uses(stmt);
            // if we are REDEFINING a phi variable it is a looping stmt.
            if(pv.defines_phi_var(stmt) && !values.isEmpty()) {
                found_link = true;
                Logger.debug("Found that stmt '" + stmt.toString()  + "' links to phi stmt '" + pv.toString() + "'.");
                Logger.debug("This is most likely a Looping stmt, also needs to be used as an index to be useful.");
                pv.add_linked_stmt(stmt);
                // TODO: not sure if this is correct...
                pv.add_alias(left, stmt, values);
            }
            else if(!values.isEmpty() && !Utils.is_def(stmt)) {
                // if we we are not DEFINING a phi var but we are using one
                found_link = true;
                Logger.debug("Found that stmt '" + stmt.toString() + "' uses phi vars:");
                Logger.debug("\toriginal phi: " + pv.toString());
                for(ImmutablePair<Value, Value> v_pair : values) {
                    Logger.debug("\t  " + v_pair.getLeft().toString() + " is effected by " + v_pair.getRight().toString());
                }
                pv.add_alias(left, stmt, values);
            }
            if(stmt.containsArrayRef()) {
                ArrayRef ar = stmt.getArrayRef();
                String index_name = ar.getIndexBox().getValue().toString();
                // Logger.info(pv.toString() + " has been " + index_name + ": " + );
                if(Objects.equals(pv.get_phi_def().getValue().toString(), index_name)
                        || pv.has_ever_been(index_name)) {
                    Logger.debug("PhiVar " + index_name + " used as an index, needs to also have a link");
//                    found_link = true;
                    pv.set_used_as_index(true);
                }
            }
        }
        return found_link;
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
     * @param constants a list of variables that are constants (phi values do not effect them at all)
     * @param v the variable name
     */
    void print_var_dep_chain(Set<String> constants, String v) {
        // TODO: this is a proof of concept function
        for(PhiVariable pv : phi_vars) {
            if(pv.has_ever_been(v)) {
                String s = pv.get_var_dep_chain_str(constants, v);
                if(Utils.not_null(s)) {
                    // make sure it is not null!
                    Logger.info(s);
                }
            }
        }
    }

    /**
     * make a variable dependency graph for the passed variable
     * @param constants a list of variables that are constants (phi values do not effect them at all)
     * @param v the variable name
     */
    void make_var_dep_chain_graph(Set<String> constants, String v) {
        for(PhiVariable pv : phi_vars) {
            if(pv.has_ever_been(v)) {
               pv.make_var_dep_chain_graph(constants, v);
            }
        }
    }

    /**
     * get the actual list of assignments for the passed variable
     * @param constants a list of variables that are constants (phi values do not effect them at all)
     * @param v the variable name
     * @return a pair consisting of the variable and the list of assignment statements (if it exists)
     *   otherwise null.
     */
    ImmutablePair<Variable, List<AssignStmt>> get_var_dep_chain(Set<String> constants, String v) {
        for(PhiVariable pv : phi_vars) {
            if(pv.has_ever_been(v)) {
                ImmutablePair<Variable, Set<AssignStmt>> ans = pv.get_var_dep_chain(constants, v);
                // TODO: this now comes out unordered... fix this
                return new ImmutablePair<>(ans.getLeft(), new ArrayList<>(ans.getRight()));
            }
        }
        return null;
    }



    /**
     * get a list of looping index vars
     * @return a list of all looping index vars
     */
    List<PhiVariable> get_looping_index_vars() {
        List<PhiVariable> pv_lst = new ArrayList<>();
        for(PhiVariable pv : phi_vars) {
            if(pv.is_looping_index_var()) {
                pv_lst.add(new PhiVariable(pv));
            }
        }
        return pv_lst;
    }

    /**
     * get all phi variables that are NOT index variables (used for SCC)
     * @return the list of phi variables that are NOT index variables
     */
    List<PhiVariable> get_non_index_vars() {
        return phi_vars.stream().filter(pv -> !pv.is_used_as_index()).collect(Collectors.toList());
    }

    /**
     * get the list of phi variables
     * @return the list of phi variables
     */
    Set<PhiVariable> get_phi_vars() {
        return phi_vars;
    }

    /**
     * get a phi variable that has a link that defines the passed variable
     * @param link the passed variable
     * @return either null (if no phi variable contains the definition) or the phi variable containing the def
     */
    PhiVariable get_phi_var_with_link_def(String link) {
        List<PhiVariable> pv = this.phi_vars.stream()
                .filter(el -> el.has_linked_var_def(link)).collect(Collectors.toList());
        if(pv.size() == 1) {
            return pv.get(0);
        } else {
            Logger.debug("This value should be 1: " + pv.size());
            return null;
        }
    }

    /**
     * get a phi variable if it is in the container
     * @param def the definition name of the variable
     * @return the phi variable if it is in the container. If not returns null
     */
    PhiVariable get_phi_var(String def) {
        for(PhiVariable pv : phi_vars) {
            if(Objects.equals(pv.get_phi_def().getValue().toString(), def)) {
                return pv;
            }
        }
        return null;
    }


    /**
     * get the phi expression associated with a phi variable
     * @param var the variable
     * @return the phi expression iff there is a var defines a phi function otherwise null
     */
    PhiExpr get_phi_expr(String var) {
        for(PhiVariable pv : phi_vars) {
            if(Objects.equals(var, pv.get_phi_def().getValue().toString())) {
                return pv.get_phi_expr();
            }
        }
        return null;
    }

    /**
     * true iff there is a phi variable that uses this variable in it's definition
     * @param var the variable in question
     * @return true iff there is a phi variable that uses this variable in it's definition
     */
    boolean is_used_in_phi(String var) {
        for(PhiVariable pv : phi_vars) {
            if(pv.get_uses().contains(var)) {
                return true;
            }
        }
        return false;
    }
}
