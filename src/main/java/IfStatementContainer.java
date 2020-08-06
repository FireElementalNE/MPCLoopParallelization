import soot.jimple.IfStmt;

import java.util.HashMap;
import java.util.Map;

/**
 * a container class for if statements
 */
class IfStatementContainer {
    /**
     * container for the IfStmts the key is the IfStmt toString()
     */
    Map<String, IfStatement> statements;

    /**
     * Constructor
     */
    IfStatementContainer() {
        statements = new HashMap<>();
    }

    /**
     * copy constructor
     * @param b_stmts the other IfStatementContainer
     */
    IfStatementContainer(IfStatementContainer b_stmts) {
        this.statements = new HashMap<>(b_stmts.statements);
    }

    /**
     * add an element to the container
     * @param ifstatement the IfStatement
     */
    void add(IfStatement ifstatement) {
        statements.put(ifstatement.get_stmt().toString(), ifstatement);
    }

    /**
     * get an IfStmt from the container (String)
     * @param s a String representation of the IfStmt
     * @return the element or null;
     */
    IfStatement get(String s) {
        return statements.get(s);
    }

    /**
     * get an IfStatement from the container (IfStmt)
     * @param stmt the IfStmt
     * @return the element or null;
     */
    IfStatement get(IfStmt stmt) {
        return statements.get(stmt.toString());
    }

    /**
     * test for membership in the container (IfStmt)
     * @param stmt the IfStmt
     * @return true iff the stmt is in the container
     */
    boolean contains_statement(IfStmt stmt) {
        return statements.containsKey(stmt.toString());
    }

    /**
     * test for membership in the container (String)
     * @param s a String representation of the IfStmt
     * @return true iff the stmt is in the container
     */
    boolean contains_statement(String s) {
        return statements.containsKey(s);
    }

    /**
     * get the map of if statements
     * @return the map of if stmts
     */
    Map<String, IfStatement> get_statements(){
        return new HashMap<>(statements);
    }

}
