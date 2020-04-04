import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.HashSet;
import java.util.Set;

public class VariableVisitor extends AbstractStmtSwitch {

    private PhiVariableContainer phi_vars;
    private Set<String> top_phi_var_names;
    /**
     * create new VariableVisitor
     * this class is looks for possible index values and tracks them
     * Index values are either:
     *   1. Constants
     *   2. Have a Phi variable somewhere in their def chain
     * @param phi_vars a container containing all phi_variables that have been seen up to this point
     *                 (along with the aliases of those PhiVariables
     * @param top_phi_var_names This is a convenience set to keep track of the original phi variable names,
     *                          this is used when parsing the second iteration.
     */
    VariableVisitor(PhiVariableContainer phi_vars, Set<String> top_phi_var_names) {
        this.phi_vars = new PhiVariableContainer(phi_vars);
        this.top_phi_var_names = new HashSet<>(top_phi_var_names);
    }

    /**
     * get the (possibly) modified phi variable container  (this is called after everything has finished)
     * @return the phi variable container
     */
    PhiVariableContainer get_phi_vars() {
        return new PhiVariableContainer(phi_vars);
    }

    /**
     * getter for the top level phi variable names used in the second
     * @return the set of top level phi variable names;
     */
    Set<String> get_top_phi_var_names() {
        return new HashSet<>(top_phi_var_names);
    }

    /**
     * Visitor ran across an assignment statement, overriding def in AbstractStmtSwitch
     * This is the main method that parses variables
     * @param stmt the assignment statement
     */
    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        ValueBox right = stmt.getRightOpBox();
        if(right.getValue() instanceof PhiExpr) {
            // getting a brand new phi variable
            Logger.debug("We found a phi node: " + stmt.toString());
            phi_vars.add(new PhiVariable(stmt));
            top_phi_var_names.add(stmt.getLeftOp().toString());
        } else {
            Logger.debug("Not a phi node, looking for links: " + stmt.toString());
            Logger.debug("Checking phi_vars");
            // loop through phi variables
            phi_vars.process_assignment(stmt);
        }
        /* TODO:
        From here we need to do something like the following.

        When we get an assignment stmt:
            1) check if ANYTHING on the right side is linked to a variable that is in a phi node; if so
                then will effect our _d_. (these are the equations that we will pass to z3 eventually...)
            2) somehow link ALL of those statements to the phi node...
            3) at the end when all of the statements are linked to the phi node, pass to z3 and get a concrete
                answer (if it exists....)
            4) ...?
         */
    }
}