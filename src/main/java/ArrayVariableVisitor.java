import org.tinylog.Logger;
import soot.jimple.*;
import soot.jimple.internal.JNewArrayExpr;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Visitor for keeping track of array Variables
 */
public class ArrayVariableVisitor extends AbstractStmtSwitch {
    /**
     * all array variables
     */
    private final ArrayVariables vars;
    /**
     * the array definition use graph
     */
    private final ArrayDefUseGraph graph;
    /**
     * the number of the block of code being currently analyzed
     */
    private final int block_num;
    /**
     * flag that is used by VariableVisitor to check for constants. If we are dealing with an array
     * it is not a constant we are interested in.
     */
    private boolean is_array;
    /**
     * set of new array statements
     */
    private final Set<Stmt> new_array_stmts;
    /**
     * construct to get shimple lines
     */
    private BodyLineFinder blf;


    /**
     * Create a new array variable visitor
     * This class searches for array definitions and uses, tracks them and adds them to the
     * def/use graph object. This mostly handles array variable versioning.
     * @param vars the current map of array variables coupled with their versions  when this visitor is called
     *             this is wrapped in the ArrayVariables class
     * @param graph the current array def/use graph when this visitor is called
     * @param block_num the block number that is calling this visitor
     * @param new_array_stmts set of new array stmts
     * @param blf construct to get line numbers correctly
     */
    ArrayVariableVisitor(ArrayVariables vars, ArrayDefUseGraph graph, int block_num,
                         Set<Stmt> new_array_stmts, BodyLineFinder blf) {
        this.vars = new ArrayVariables(vars);
        this.graph = new ArrayDefUseGraph(graph);
        this.block_num = block_num;
        this.is_array = false;
        this.new_array_stmts = new HashSet<>(new_array_stmts);
        this.blf = blf;
    }

    /**
     * get the  array variables (this is called after everything has finished)
     * @return the wrapper class for the map of array variables and versions
     */
    ArrayVariables get_vars() {
        return new ArrayVariables(vars);
    }

    /**
     * the the modified def/use graph (this is called after everything has finished)
     * @return the array def/use graph object
     */
    ArrayDefUseGraph get_graph() {
        return graph;
    }

    /**
     * getter for new array stmts
     * @return new array stmts set
     */
    Set<Stmt> get_new_array_stmts() {
        return new HashSet<>(new_array_stmts);
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
            if(!vars.contains_key(basename)) {
                vars.put(basename, new ArrayVersionSingle(0, block_num, stmt, blf.get_line(stmt)));
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
//        List<ValueBox> uses = stmt.getUseBoxes();
        if(stmt.containsArrayRef()) {
            check_array_ref(stmt);
        }
        // TODO: many more ways to create new arrays, see DB_JOIN3.java...
        if(stmt.getRightOp() instanceof JNewArrayExpr) {
            // NOTE: ALWAYS a new array!
            Logger.info("Found a new array: " + left_op);
            ArrayVersion av = new ArrayVersionSingle(0, block_num, stmt, blf.get_line(stmt));
            graph.add_node(new Node(stmt, left_op, av, new ArrayIndex(), DefOrUse.DEF, true,  blf.get_line(stmt)),
                    true, false);
            vars.put(left_op, av);
            new_array_stmts.add(stmt);
            is_array = true;
        }
        else if(vars.contains_key(right_op)) {
            // NOTE: PURE array renaming!
            assert !stmt.containsArrayRef() : "pure array naming cannot contain an array reference.";
            ArrayVersion av = vars.get(right_op);
            ArrayVersion new_av = Utils.rename_av(av);
            vars.put(left_op, new_av);
            graph.array_def_rename(right_op, vars.get(right_op), left_op, new_av, stmt, true, stmt instanceof IfStmt, blf);
            vars.remove(right_op);
            Logger.debug("An array got renamed...");
            is_array = true;
        }
        else if(stmt.containsArrayRef()
                && Objects.equals(left_op, stmt.getArrayRef().toString())
                && vars.contains_key(get_basename(stmt.getArrayRef()))) {
            Logger.debug("Array " + get_basename(stmt.getArrayRef()) + " got written to.");
            vars.toggle_written(get_basename(stmt.getArrayRef()));
        } else if(stmt.containsArrayRef()
                && !Objects.equals(left_op, stmt.getArrayRef().toString())
                && vars.contains_key(get_basename(stmt.getArrayRef()))) {
            Logger.debug("Array " + get_basename(stmt.getArrayRef()) + " got read.");
            vars.toggle_read(get_basename(stmt.getArrayRef()));
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
