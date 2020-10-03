import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.shimple.PhiExpr;

import java.util.*;

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
     * set of new array statements
     */
    private final Set<Stmt> new_array_stmts;
    /**
     * map to handle array USES being used in IFSTMTS (and therefore MUX stmts)
     */
    private final Map<String, ValueBox> array_reads_for_if_stmts;

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
     * @param new_array_stmts set of new array stmts
     * @param array_reads_for_if_stmts map to handle array USES being used in IFSTMTS (and therefore MUX stmts)
     */
    VariableVisitor(PhiVariableContainer phi_vars, Set<String> top_phi_var_names, Map<String, Integer> constants,
                    boolean is_array, boolean in_loop, boolean is_merge, Set<Stmt> new_array_stmts,
                    Map<String, ValueBox> array_reads_for_if_stmts) {
        this.phi_vars = new PhiVariableContainer(phi_vars);
        this.top_phi_var_names = new HashSet<>(top_phi_var_names);
        this.is_array = is_array;
        this.in_loop = in_loop;
        this.constants = new HashMap<>(constants);
        this.is_merge = is_merge;
        this.new_array_stmts = new HashSet<>(new_array_stmts);
        this.array_reads_for_if_stmts = new HashMap<>(array_reads_for_if_stmts);
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
     * getter for new array stmts
     * @return new array stmts set
     */
    Set<Stmt> get_new_array_stmts() {
        return new HashSet<>(new_array_stmts);
    }

    /**
     * getter for map to handle array USES being used in IFSTMTS
     * @return the map of array uses
     */
    Map<String, ValueBox> get_array_reads_for_if_stmts() {
        return new HashMap<>(array_reads_for_if_stmts);
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
        if(new_array_stmts.contains(stmt)) {
            // TODO: test this
            Logger.debug("Skipping new array statement: " + stmt.toString());
        } else if(right.getValue() instanceof PhiExpr) {
            // getting a brand new phi variable
            Logger.debug("We found a phi node: " + stmt.toString());
            phi_vars.add(new PhiVariable(stmt));
            top_phi_var_names.add(stmt.getLeftOp().toString());
            if (is_merge) {
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
            } else if(found_link) {
                Logger.debug("Found phi link in stmt: " + stmt.toString());
            } else if(stmt.containsArrayRef() && Objects.equals(stmt.getArrayRefBox().toString(), stmt.getLeftOpBox().toString())) {
                Logger.debug("found an array write that does not link to a phi stmt: " + stmt.toString());
            } else if(stmt.containsArrayRef() && Objects.equals(stmt.getArrayRefBox().toString(), stmt.getRightOpBox().toString())) {
                Logger.info("found an array READ that could be used in an if statement: " + stmt.toString());
                array_reads_for_if_stmts.put(stmt.getLeftOp().toString(), stmt.getRightOpBox());
            } else {
                // TODO: this needs to be handled creates error on an array read that is used in an if stmt
                //        also errors out on new arrays (check comment in BFSVisitor line 181
                Logger.error("case not handled properly: " + stmt.toString());
//                System.exit(0);
            }
        }
    }
}