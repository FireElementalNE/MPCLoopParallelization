import soot.jimple.AbstractStmtSwitch;
import soot.jimple.IfStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.Stack;

/**
 * visitor to match if statements with their true and false branches using the CFG
 */
class IfStatementVisitor extends AbstractStmtSwitch {

    /**
     * the CFG
     */
    private final ExceptionalUnitGraph g;

    /**
     * a container holding all of the previously found IfStmts
     */
    private final IfStatementContainer if_stmts;

    /**
     * condition stack
     */
    private final Stack<IfStmt> cond_stk;

    /**
     * flag that is true iff we are analyzing the head node
     */
    boolean is_head;

    /**
     * Constructor
     * @param g the CFG
     * @param if_stmts the previously parsed Branch Statements
     * @param cond_stk the stack of conditions (used for mux nodes)
     */
    IfStatementVisitor(ExceptionalUnitGraph g, IfStatementContainer if_stmts,
                       Stack<IfStmt> cond_stk) {
        this.g = g;
        this.if_stmts = new IfStatementContainer(if_stmts);
        this.cond_stk = cond_stk;
        this.is_head = is_head;
    }

    /**
     * getter for b_stmts
     * @return the possible modified IfStatementContainer
     */
    IfStatementContainer get_if_stmts() {
        return new IfStatementContainer(if_stmts);
    }

    /**
     * getter for condition stack
     * @return the possibly modified condition stack
     */
    Stack<IfStmt> get_cond_stk() {
        return cond_stk;
    }


    /**
     * fall through case for an ifstmt
     * @param stmt the if statement
     */
    @Override
    public void caseIfStmt(IfStmt stmt) {
        if(!if_stmts.contains_statement(stmt)) {
            if_stmts.add(new IfStatement(stmt, g.getSuccsOf(stmt)));
        }
        cond_stk.push(stmt);
    }
}
