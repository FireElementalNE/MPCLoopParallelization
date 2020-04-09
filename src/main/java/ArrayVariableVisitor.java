import org.tinylog.Logger;
import soot.jimple.*;
import soot.jimple.internal.JNewArrayExpr;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Visitor for keeping track of array Variables
 */
public class ArrayVariableVisitor extends AbstractStmtSwitch {
    private Map<String, ArrayVersion> vars;
    private ArrayDefUseGraph graph;
    private int block_num;
    // flag that is used by VariableVisitor to check for constants. If we are dealing with an array
    // it is not a constant we are interested in.
    private boolean is_array;

    /**
     * Create a new array variable visitor
     * This class searches for array definitions and uses, tracks them and adds them to the
     * def/use graph object. This mostly handles array variable versioning.
     * @param vars the current map of array variables coupled with their versions  when this visitor is called
     * @param graph the current array def/use graph when this visitor is called
     * @param block_num the block number that is calling this visitor
     */
    ArrayVariableVisitor(Map<String, ArrayVersion> vars, ArrayDefUseGraph graph, int block_num) {
        this.vars = new HashMap<>();
        this.vars =  vars.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.graph = new ArrayDefUseGraph(graph);
        this.block_num = block_num;
        this.is_array = false;
    }

    /**
     * get a list of the list of array variables (this is called after everything has finished)
     * @return the map of array variables and versions
     */
    Map<String, ArrayVersion> get_vars() {
        return vars.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * the the modified def/use graph (this is called after everything has finished)
     * @return the array def/use graph object
     */
    ArrayDefUseGraph get_graph() {
        return graph;
    }

    /**
     * return is array field
     * @return true iff we are dealing with an array.
     */
    boolean get_is_array() {
        return is_array;
    }

    /**
     * get tje basename pf an array ref (just to make code easier to read)
     * @param aref the array reference
     * @return the basename of the array reference
     */
    private String get_basename(ArrayRef aref) {
        return aref.getBaseBox().getValue().toString();
    }

    /**
     * check an array ref to see if it is contained in the map of array variables/array versions
     * @param stmt the current stmt being analyzed
     */
    private void check_array_ref(Stmt stmt) {
        if(stmt.containsArrayRef()) {
            is_array = true;
            String basename = get_basename(stmt.getArrayRef());
            if(!vars.containsKey(basename)) {
                vars.put(basename, new ArrayVersionSingle(1, block_num));
            }
        }
    }

    /**
     * handle breakpoint stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle invoke stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle assignment stmts (inherited from AbstractStmtSwitch)
     * this is one of the two main functions. If we have an assignment we could be
     *    1. defining a new array
     *    2. renaming an array
     *    3. referencing an array element that might be in the array variable/array version map
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        String left_op = stmt.getLeftOp().toString();
        String right_op = stmt.getRightOp().toString();
        if(stmt.containsArrayRef()) {
            check_array_ref(stmt);
        }
        // TODO: many more ways to create new arrays, see DB_JOIN3.java...
        if(stmt.getRightOp() instanceof JNewArrayExpr) {
            // NOTE: ALWAYS a new array!
            Logger.info("Found a new array: " + left_op);
            ArrayVersion av = new ArrayVersionSingle(1, block_num);
            graph.add_node(new Node(stmt.toString(), left_op, av, new Index(), DefOrUse.DEF, stmt.getJavaSourceStartLineNumber()),
                    true, false);
            vars.put(left_op, av);
            is_array = true;
        }
        if(vars.containsKey(right_op)) {
            // NOTE: PURE array renaming!
            assert !stmt.containsArrayRef();
            ArrayVersion av = vars.get(right_op);
            ArrayVersion new_av = Utils.rename_av(av);
            vars.put(left_op, new_av);
            graph.array_def_rename(right_op, vars.get(right_op), left_op, new_av, stmt);
            vars.remove(right_op);
            Logger.debug("An array got renamed...");
            is_array = true;
        }
    }

    /**
     * handle identity stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle enter monitor stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle exit monitor stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle goto stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle if stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseIfStmt(IfStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle lookup switch stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle NOP stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseNopStmt(NopStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle ret stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseRetStmt(RetStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle return stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle return void stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle table switch stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        check_array_ref(stmt);
    }

    /**
     * handle throw stmts (inherited from AbstractStmtSwitch)
     * @param stmt the stmt being analyzed
     */
    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        check_array_ref(stmt);
    }
}
