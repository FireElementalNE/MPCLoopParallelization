import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.shimple.PhiExpr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A visitor class that looks at possible index values
 */
@SuppressWarnings("FieldMayBeFinal")
public class VariableVisitor extends AbstractStmtSwitch {

    private PhiVariableContainer phi_vars;
    private Set<String> top_phi_var_names;
    private boolean is_array;
    private boolean in_loop;
    private Map<String, Integer> constants;
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
     * @param constants a set of constants seen in non-loop blocks
     * @param is_array used to find constants when we are _outside_ of a loop body
     * @param in_loop true iff this is called when processing inside of a loop
     */
    VariableVisitor(PhiVariableContainer phi_vars, Set<String> top_phi_var_names, Map<String, Integer> constants,
                    boolean is_array, boolean in_loop) {
        this.phi_vars = new PhiVariableContainer(phi_vars);
        this.top_phi_var_names = new HashSet<>(top_phi_var_names);
        this.is_array = is_array;
        this.in_loop = in_loop;
        this.constants = new HashMap<>(constants);
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
     * get possible changed constants list (only used when not in a loop)
     * @return the set of constants
     */
    Map<String, Integer> get_constants() {
        assert !in_loop;
        return new HashMap<>(constants);
    }

    /**
     * function to check a statement (within a loop) to see if it is composed of _only_ constants
     * if it is add all of the uses and the def to the constant set
     * @param stmt the statement
     */
    void check_constants(AssignStmt stmt) {
        String right = stmt.getRightOp().toString();
        for(Map.Entry<String, Integer> entry : constants.entrySet()) {
            right = right.replace(entry.getKey(), entry.getValue().toString());
        }
        int result = 0;
        try {
            Expression e = new ExpressionBuilder(right).build();
            result = (int) e.evaluate();
        } catch (net.objecthunter.exp4j.tokenizer.UnknownFunctionOrVariableException e) {
            Logger.error(e.getMessage());
            Logger.error(right);
            System.exit(0);
        }
        constants.put(stmt.getLeftOp().toString(), result);
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
            boolean found_link = phi_vars.process_assignment(stmt);
            if(!found_link && !stmt.containsArrayRef() && !is_array) {
                if(!in_loop) {
                    Logger.debug("'" + stmt.toString() + "' appears to deal with a constant");
                    if(stmt.getUseBoxes().size() == 1 && NumberUtils.isCreatable(stmt.getRightOp().toString())) {
                        constants.put(stmt.getLeftOp().toString(), Integer.parseInt(stmt.getRightOp().toString()));
                    } else {
                        Logger.error("Not sure what went wrong. Something did.");
                    }
//                    constants.add(stmt.getLeftOp().toString());
                } else {
                    check_constants(stmt);
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