import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.*;
import soot.jimple.internal.ConditionExprBox;
import soot.toolkits.graph.Block;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The main visitor for the BFS algorithm.
 */
class BFSVisitor extends AbstractStmtSwitch {
    /**
     * the container class that holds all non array phi variables
     */
    private final PhiVariableContainer phi_vars;
    /**
     * Map that represents the current array versions that a block presents to a successor block
     */
    private final Map<Block, DownwardExposedArrayRef> c_arr_ver;
    /**
     * The current block being analyzed
     */
    private final Block b;
    /**
     * the current downward exposed array references from the previous block
     */
    private final DownwardExposedArrayRef daf;
    /**
     * The final DefUse Graph
     */
    private final ArrayDefUseGraph graph;
    /**
     * A wrapper class that contains a Map for all array variables to  array version
     */
    private final ArrayVariables array_vars;
    /**
     * the block number for the merge
     */
    private final int block_num;
    /**
     * the constants
     */
    private final Map<String, Integer> constants;
    /**
     * Constructor for the BFS Visitor
     * @param c_arr_ver a Map of currently exposed array versions per block
     * @param b the current block
     * @param graph the current ArrayDefUseGraph
     * @param array_vars the array variables
     * @param phi_vars a container containing all phi_variables that have been seen up to this point
     *                 (along with the aliases of those PhiVariables
     * @param constants the constants
     * @param block_num the number of the block
     */
    BFSVisitor(Map<Block, DownwardExposedArrayRef> c_arr_ver, Block b, ArrayDefUseGraph graph,
               ArrayVariables array_vars, PhiVariableContainer phi_vars, Map<String, Integer> constants,
               int block_num) {
        this.c_arr_ver = c_arr_ver;
        this.b = b;
        assert c_arr_ver.containsKey(b) : "the current array versions must have an entry for the current block";
        this.daf = new DownwardExposedArrayRef(c_arr_ver.get(b));
        this.graph = new ArrayDefUseGraph(graph);
        this.block_num = block_num;
        this.array_vars = new ArrayVariables(array_vars);
        this.phi_vars = phi_vars;
        this.constants = constants;
    }

    /**
     * getter for the c_arr_ver Map
     * @return the c_arr_ver Map
     */
    Map<Block, DownwardExposedArrayRef> get_c_arr_ver() {
        return c_arr_ver;
    }

    /**
     * getter for the possible change ArrayDefUseGraph
     * @return the ArrayDefUseGraph
     */
    ArrayDefUseGraph get_graph() {
        return graph;
    }

    /**
     * return the possibly changed array vars
     * @return the array vars
     */
    ArrayVariables get_vars() {
        return new ArrayVariables(array_vars);
    }


    /**
     * TODO: not needed
     * check a non-assignment statement for array reads from aliased variables
     * @param stmt the statement
     * @return true iff there is an array read
     */
    @SuppressWarnings("unused")
    boolean check_for_array_ref(Stmt stmt) {
        List<ValueBox> vals = stmt.getUseBoxes().stream().filter(el -> !(el instanceof ConditionExprBox)).collect(Collectors.toList());
        for(ValueBox vb : vals) {
            String name = vb.getValue().toString();
            if(!NumberUtils.isCreatable(name)) {
                ImmutablePair<Variable, List<AssignStmt>> dep_chain = phi_vars.get_var_dep_chain(constants, name);
                if(Utils.not_null(dep_chain)) {
                    if(!Utils.not_null(dep_chain.getRight()) && !Utils.not_null(dep_chain.getLeft())) {
                        // it is a constant
                        return false;
                    } else {
                        for (AssignStmt as : dep_chain.getRight()) {
                            if (as.containsArrayRef()) {
                                return true;
                            }
                        }
                    }
                } else {
                    Logger.error("Dep chain was null for stmt: " + stmt.toString() + "(" + name + ")");
                }

            } else {
                Logger.info("This is a number.");
            }
        }
        return false;
    }

    /**
     * Check and an array USE and add:
     *    1. Changed DownwardExposedArrayRef for this block
     *    2. Usage Node
     *    3. Possible add Edge
     * @param stmt the current Statement
     */
    private void check_array_read(Stmt stmt) {
        if(stmt instanceof AssignStmt) {
            String basename = stmt.getArrayRef().getBaseBox().getValue().toString();
            ValueBox index_box = stmt.getArrayRef().getIndexBox();
            Logger.debug("Change needed in stmt: " + stmt.toString());
            Logger.debug(" The index is: " + index_box.getValue().toString());
            Logger.debug(" " + basename + " should be changed to " + daf.get_name(basename));
            Logger.debug(" " + "This is a use for " + daf.get_name(basename));
            ArrayVersion av = Utils.copy_av(daf.get(basename));
            Node new_node = new Node(stmt, basename, av, new ArrayIndex(index_box), DefOrUse.USE,
                    new ImmutablePair<>(basename, daf.get_name(basename)), false);
            graph.add_node(new_node, false, false);
            c_arr_ver.put(b, daf);
            array_vars.toggle_read(basename);
        } else if (stmt instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt)stmt;
                ValueBox cond_exp_box = ifStmt.getConditionBox();
                // List<ValueBox> vals = stmt.getUseBoxes().stream().filter(el -> !(el instanceof ConditionExprBox)).collect(Collectors.toList());
                List<ValueBox> vals = cond_exp_box.getValue().getUseBoxes();
                for(ValueBox vb : vals) {
                    String name = vb.getValue().toString();
                    if(!NumberUtils.isCreatable(name)) {
                        ImmutablePair<Variable, List<AssignStmt>> dep_chain = phi_vars.get_var_dep_chain(constants, name);
                        for(AssignStmt as : dep_chain.getRight()) {
                            if(as.containsArrayRef()) {
                                Logger.info("IF STMT: here is the assignment: " + as.toString());
                                String basename = as.getArrayRef().getBaseBox().getValue().toString();
                                ValueBox index_box = as.getArrayRef().getIndexBox();
                                ArrayVersion av = Utils.copy_av(daf.get(basename));
                                Node new_node = new Node(stmt, basename, av, new ArrayIndex(index_box), DefOrUse.USE,
                                        new ImmutablePair<>(basename, daf.get_name(basename)), false);
                                graph.add_node(new_node, false, false);
                                array_vars.toggle_read(basename);
                            }
                        }
                    }
                }
        }
    }

    /**
     * Check AssignStmt statement for possible Definitions and Usages
     * @param stmt the statement
     */
    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if(stmt.containsArrayRef()) {
//            ValueBox left = stmt.getLeftOpBox();
            if(Utils.is_def(stmt)) {
                String basename = stmt.getArrayRef().getBaseBox().getValue().toString();
                // TODO: get deps for right hand side of assignment.... see HPL_dgemm1
                //       started to fix, see ArrayDefUseGraph:print_def_node_dep_chains
                Logger.debug("Array write found (change needed): " + stmt.toString());
                daf.new_ver(basename, block_num, stmt);
                ArrayVersion av = Utils.copy_av(daf.get(basename));
                if(graph.get_nodes().containsKey(Node.make_id(basename, av, DefOrUse.DEF))) {
                    Logger.warn("Id conflict, forcing version increase before adding node");
                    daf.force_incr(basename);
                    av.force_incr_version();
                }
                Logger.debug(" " + basename + " needs to be changed to " + daf.get_name(basename));
                Logger.debug("This is a new def for " + daf.get_name(basename));
                array_vars.put(basename, av);
                graph.add_node(new Node(stmt, basename, av, new ArrayIndex(stmt.getArrayRef().getIndexBox()), DefOrUse.DEF,
                        new ImmutablePair<>(basename, daf.get_name(basename)), false), true, false);
                c_arr_ver.put(b, daf);
            } else {
                check_array_read(stmt);
            }
        }
    }

    /**
     * Check InvokeStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check IdentityStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check GotoStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check IfStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseIfStmt(IfStmt stmt) {
        if(check_for_array_ref(stmt)) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check LookupSwitchStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check ReturnStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check TableSwitchStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }

    /**
     * Check ThrowStmt statement
     * @param stmt the statement
     */
    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("Checking " + stmt.toString());
            check_array_read(stmt);
        }
    }
    /**
     * Check BreakpointStmt statement (NOT IMPLEMENTED)
     * @param stmt the statement
     */
    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {

    }

    /**
     * Check EnterMonitorStmt statement (NOT IMPLEMENTED)
     * @param stmt the statement
     */
    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {

    }

    /**
     * Check ExitMonitorStmt statement (NOT IMPLEMENTED)
     * @param stmt the statement
     */
    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {

    }


    /**
     * Check NopStmt statement (NOT IMPLEMENTED)
     * @param stmt the statement
     */
    @Override
    public void caseNopStmt(NopStmt stmt) {

    }

    /**
     * Check RetStmt statement (NOT IMPLEMENTED)
     * @param stmt the statement
     */
    @Override
    public void caseRetStmt(RetStmt stmt) {

    }

    /**
     * Check ReturnVoidStmt statement (NOT IMPLEMENTED)
     * @param stmt the statement
     */
    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {

    }

    /**
     * default case (NOT IMPLEMENTED)
     * @param obj the object?
     */
    @Override
    public void defaultCase(Object obj) {

    }
}
