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
 * A visitor class that looks at possible index values and handles non array phi node MUX transformations
 */
public class VariableVisitor extends AbstractStmtSwitch {
    /**
     * the container class that holds all non array phi variables
     */
    private final PhiVariableContainer phi_vars;
    /**
     * flag to determine if constants need to be looked for
     */
    private final boolean is_array;
    /**
     * flag to determine if we are in a loop body
     */
    private final boolean in_loop;
    /**
     * A set of possible constants gathered from non-loop blocks
     */
    private final Map<String, Integer> constants;
    /**
     * A list of the _original_ phi variables that is queried on the second iteration
     */
    private final Set<String> top_phi_var_names;
    /**
     * flag to indicate a merge node
     */
    private final boolean is_merge;

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
     * @param is_merge flag to indicate if node is part of a merge block
     */
    VariableVisitor(PhiVariableContainer phi_vars, Set<String> top_phi_var_names, Map<String, Integer> constants,
                    boolean is_array, boolean in_loop, boolean is_merge) {
        this.phi_vars = new PhiVariableContainer(phi_vars);
        this.top_phi_var_names = new HashSet<>(top_phi_var_names);
        this.is_array = is_array;
        this.in_loop = in_loop;
        this.constants = new HashMap<>(constants);
        this.is_merge = is_merge;
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
//        assert !in_loop : "we only update constants if we are not in a loop";
        return new HashMap<>(constants);
    }

    /**
     * function to check a statement (within a loop) to see if it is composed of _only_ constants
     * if it is add all of the uses and the def to the constant set
     * @param stmt the statement
     */
    void check_constants(AssignStmt stmt) {
        String right = stmt.getRightOp().toString();
        String left =  stmt.getLeftOp().toString();
        right = right.replace("$", "");
        left = left.replace("$", "");
        Logger.info("Checking if '" + stmt.getLeftOp().toString() + "' is a constant.");
        for(Map.Entry<String, Integer> entry : constants.entrySet()) {
            right = right.replace(entry.getKey(), entry.getValue().toString());
        }
        int result = 0;
        try {
            Expression e = new ExpressionBuilder(right).build();
            result = (int) e.evaluate();
        } catch (net.objecthunter.exp4j.tokenizer.UnknownFunctionOrVariableException e) {
            Logger.error("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if(Constants.PRINT_ST) {
                e.printStackTrace();
            }
        }
        Logger.debug("\teq: " + right + " -> " + right + " = " + result);
        Logger.info("'" + right + "' is a constant.");
        constants.put(left, result);
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
            if(is_merge) {
                //TODO: continue mux here
                PhiExpr pexpr = (PhiExpr) stmt.getRightOp();
            }
        } else {
            Logger.debug("Not a phi node, looking for links: " + stmt.toString());
            Logger.debug("Checking phi_vars");

            // loop through phi variables
            boolean found_link = phi_vars.process_assignment(stmt);
            if(!found_link && !stmt.containsArrayRef() && !is_array) {
                if(!in_loop) {
                    Logger.debug("'" + stmt.toString() + "' appears to deal with a constant");
                    String left_stmt = stmt.getLeftOp().toString().replace("$", "");
                    String right_stmt = stmt.getRightOp().toString().replace("$", "");
                    if(stmt.getUseBoxes().size() == 1) {
                        if(NumberUtils.isCreatable(right_stmt)) {
                            constants.put(left_stmt, Integer.parseInt(right_stmt));
                        } else if(constants.containsKey(right_stmt)) {
                            constants.put(left_stmt, constants.get(right_stmt));
                        }
                    } else {
                        Logger.error("Could not add constant in stmt '" + stmt.toString());
//                        System.exit(0);
                    }
//                    constants.add(stmt.getLeftOp().toString());
                } else {
                    check_constants(stmt);
                }
            }
        }
    }
}