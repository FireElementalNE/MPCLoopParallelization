import org.tinylog.Logger;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VariableVisitor extends AbstractStmtSwitch {

    private List<Variable> vars;

    private Set<PhiVariable> phi_vars;

    /**
     * create new VariableVisitor
     * this class is looks for possible index values and tracks them
     * Index values are either:
     *   1. Constants
     *   2. Have a Phi variable somewhere in their def chain
     * @param vars a list of variables at the time the visitor is created
     * @param phi_vars a list of phi variables at the time the visitor is created
     */
    VariableVisitor(List<Variable> vars, Set<PhiVariable> phi_vars) {
        this.vars =  vars.stream().map(Variable::new).collect(Collectors.toList());
        this.phi_vars = phi_vars.stream().map(PhiVariable::new).collect(Collectors.toSet());
    }

    /**
     * get the list of variables (this is called after everything has finished
     * @return the list of variables
     */
    List<Variable> get_vars() {
        return vars.stream().map(Variable::new).collect(Collectors.toList());
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
//        ValueBox left = stmt.getLeftOpBox();
        ValueBox right = stmt.getRightOpBox();
        if(right.getValue() instanceof PhiExpr) {
            Logger.debug("We found a phi node: " + stmt.toString());
            // TODO: do we actually need to record phi links? we can always find the looping variable, so who cares.
            // TODO: see the commented out stuff in PhiVariable
//            boolean phi_links = false;
//            for(PhiVariable pv : phi_vars) {
//                if(pv.defines_phi_var(stmt)) {
//                    Logger.debug("\tnew phi node links with '" + pv.toString() + "'.");
//                    phi_links = true;
//                }
//            }
//            if(!phi_links) {
                phi_vars.add(new PhiVariable(stmt));
//            }
        } else {
            Logger.debug("Not a phi node, looking for links: " + stmt.toString());
            for(PhiVariable pv : phi_vars) {
                List<Value> values = pv.get_phi_var_uses(stmt);
                if(pv.defines_phi_var(stmt) && !values.isEmpty()) {
                    Logger.debug("Found that stmt '" + stmt.toString()  + "' links to phi stmt '" + pv.toString() + "'.");
                    Logger.debug("This is most likely a Looping stmt.");
                    pv.add_linked_stmt(stmt);
                }
                else if(!values.isEmpty()) {
                    Logger.debug("Found that stmt '" + stmt.toString() + "' uses phi vars:");
                    for(Value v : values) {
                        // I think this list should only ever be of size one....
                        vars.add(new Variable(stmt.getLeftOpBox().getValue(), stmt, v, pv.get_phi_expr()));
                        Logger.debug("\t" + stmt.getLeftOpBox().getValue() + " is effected by " + v.toString());
                    }

                }
                else if(pv.defines_phi_var(stmt)) {
                    Logger.debug("I do not think we should get here (" + stmt.toString() + ")." );
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