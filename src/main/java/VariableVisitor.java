import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VariableVisitor extends AbstractStmtSwitch {

    private Set<PhiVariable> phi_vars;

    /**
     * create new VariableVisitor
     * this class is looks for possible index values and tracks them
     * Index values are either:
     *   1. Constants
     *   2. Have a Phi variable somewhere in their def chain
     * @param phi_vars a list of phi variables at the time the visitor is created
     */
    VariableVisitor(Set<PhiVariable> phi_vars) {
        this.phi_vars = phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
    }

    /**
     * get the list of phi variables (this is called after everything has finished)
     * @return the list of phi variables
     */
    Set<PhiVariable> get_phi_vars() {
        return phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
    }

    /**
     * Visitor ran across an assignment statement, overriding def in AbstractStmtSwitch
     * This is the main method that parses variables
     * @param stmt the assignment statement
     */
    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        ValueBox left = stmt.getLeftOpBox();
        ValueBox right = stmt.getRightOpBox();
        if(right.getValue() instanceof PhiExpr) {
            // getting a brand new phi variable
            Logger.debug("We found a phi node: " + stmt.toString());
            phi_vars.add(new PhiVariable(stmt));
        } else {
            Logger.debug("Not a phi node, looking for links: " + stmt.toString());
            Logger.debug("Checking phi_vars");
            // loop through phi variables
            for(PhiVariable pv : phi_vars) {
                List<Value> values = pv.get_phi_var_uses(stmt);
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
                    for(Value v : values) {
                        Logger.debug("\t  " + stmt.getLeftOpBox().getValue() + " is effected by " + v.toString());
                    }
                    pv.add_alias(left, stmt);
                }
                else {
                    // error catch
                    Logger.error("error processing stmt: " + stmt.toString());
                }
            }

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