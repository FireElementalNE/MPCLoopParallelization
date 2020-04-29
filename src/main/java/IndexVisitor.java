import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.ValueBox;
import soot.jimple.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("FieldMayBeFinal")
public class IndexVisitor extends AbstractStmtSwitch {

    private PhiVariableContainer pvc;
    private Set<String> second_iter_def_vars;
    private Set<String> top_phi_var_names;
    private ArrayDefUseGraph graph;
    private Set<String> constants;

    IndexVisitor(PhiVariableContainer pvc, Set<String> second_iter_def_vars,
                 Set<String> top_phi_var_names, Set<String> constants, ArrayDefUseGraph graph) {
        this.pvc = new PhiVariableContainer(pvc);
        this.second_iter_def_vars = new HashSet<>(second_iter_def_vars);
        this.top_phi_var_names = new HashSet<>(top_phi_var_names);
        this.graph = new ArrayDefUseGraph(graph);
        this.constants = constants;
    }

    Set<String> get_second_iter_def_vars() {
        // TODO: this may be incorrect. To be an intra-loop dependency a variable must be based off
        //         of a phi variable, and have some sort of augmentation done to it (e.g. pv1 - 1).
        return new HashSet<>(second_iter_def_vars);
    }

    Set<String> get_top_phi_var_names() {
        return new HashSet<>(top_phi_var_names);
    }

    public ArrayDefUseGraph get_graph() {
        return new ArrayDefUseGraph(graph);
    }

    PhiVariableContainer get_pvc() {
        return new PhiVariableContainer(pvc);
    }

    private void check_index(Stmt stmt) {
        // TODO:
        //   Just check the stmt on the _right_ if it is an operation if any kind
        //   then it is some sort of intra-loop dependency
        ArrayRef ar = stmt.getArrayRef();
        ValueBox index_box = ar.getIndexBox();
        String index_name = index_box.getValue().toString();
        // now that we have all of the possible constants we should be OK.
        // only break if it is a DEF!!!
        if(!second_iter_def_vars.contains(index_name)
                && !top_phi_var_names.contains(index_name)
                && !constants.contains(index_name)) {
            Logger.debug("Printing def-chain: '" + index_name + "' in stmt '" + stmt.toString() + "'");
            pvc.print_var_dep_chain(constants, index_box.getValue().toString());
            ImmutablePair<Variable, List<AssignStmt>> dep_chain = pvc.get_var_dep_chain(constants, index_name);
            if (Utils.not_null(dep_chain)) {
                pvc.make_var_dep_chain_graph(constants, index_name);
                if(!dep_chain.getRight().isEmpty()) {
                    Logger.debug("Dep chain for " + index_name + ":");
                    for (int i = 0; i < dep_chain.getRight().size(); i++) {
                        AssignStmt a = dep_chain.getRight().get(i);
                        Logger.debug("\t" + a.toString());
                    }
                    Solver solver = new Solver(index_name, dep_chain, pvc);
                    Logger.info("Resolved dep chain: " + solver.get_resolved_eq());
//                    solver.solve();
//                    try {
//
//                        String resp = solver.send_recv_stmt();
//                        Logger.debug("Got this from server: " + resp);
//
//                    } catch (IOException e) {
//                        Logger.error("Could not send '" + stmt.toString() + "' to solver.");
//                    }
                } else {
                    Logger.debug("Dep chain for " + index_name + " is empty (is it a phi var?).");
                }
                graph.add_node(new Node(stmt.toString(), ar.getBaseBox().getValue().toString(),
                        new ArrayVersionSingle(-1, -1, stmt), new Index(index_box), DefOrUse.USE,
                        stmt.getJavaSourceStartLineNumber(), false), false, true);
            } else {
                Logger.error("dep chain for " + index_box.getValue().toString() + " is null.");
            }
            // only add if it is a def!
            if(Utils.is_def(stmt)) {
                // this must be an assign stmt at this point
                assert stmt instanceof AssignStmt;
                AssignStmt astmt = (AssignStmt)stmt;
                second_iter_def_vars.add(astmt.getLeftOp().toString());
            }
        } else {
            Logger.debug("Variable '" + index_name + "' is has been defined already.");
            Logger.debug("\t!second_iter_def_vars.contains(index_name) = " + !second_iter_def_vars.contains(index_name));
            Logger.debug("\t!top_phi_var_names.contains(index_name) = " + !top_phi_var_names.contains(index_name));
            Logger.debug("\t!constants.contains(index_name) = " + !constants.contains(index_name));
            Logger.debug("\t!Utils.is_def(stmt) = " +  !Utils.is_def(stmt));

        }
    }


    @Override
    public void caseBreakpointStmt(BreakpointStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseBreakpointStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseInvokeStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseAssignStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseIdentityStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseEnterMonitorStmt(EnterMonitorStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseEnterMonitorStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseExitMonitorStmt(ExitMonitorStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseExitMonitorStmt caseExitMonitorStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseGotoStmt(GotoStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseGotoStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseIfStmt(IfStmt stmt) {

        if(stmt.containsArrayRef()) {
            Logger.debug("caseIfStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseLookupSwitchStmt(LookupSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseLookupSwitchStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseNopStmt(NopStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseNopStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseRetStmt(RetStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseRetStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseReturnStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {

    }

    @Override
    public void caseTableSwitchStmt(TableSwitchStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseTableSwitchStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void caseThrowStmt(ThrowStmt stmt) {
        if(stmt.containsArrayRef()) {
            Logger.debug("caseThrowStmt Checking " + stmt.toString());
            check_index(stmt);
        }
    }

    @Override
    public void defaultCase(Object obj) {

    }
}
