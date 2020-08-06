import soot.jimple.AbstractStmtSwitch;
import soot.jimple.IfStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;

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
    IfStatementContainer if_stmts;

    /**
     * Constructor
     * @param g the CFG
     * @param if_stmts the previously parsed Branch Statements
     */
    IfStatementVisitor(ExceptionalUnitGraph g, IfStatementContainer if_stmts) {
        this.g = g;
        this.if_stmts = new IfStatementContainer(if_stmts);
    }

    /**
     * getter for b_stmts
     * @return the possible modified IfStatementContainer
     */
    IfStatementContainer get_b_stmts() {
        return new IfStatementContainer(if_stmts);
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
    }
}
