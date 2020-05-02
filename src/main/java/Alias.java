import soot.jimple.AssignStmt;

public class Alias {
    /**
     * the name of the alias
     */
    final private String name;
    /**
     * the assignment statement for the alias
     */
    final private AssignStmt stmt;

    /**
     * create an Alias object. An alias is an association between a variable name and an assignment stmt.
     * @param name the variable name
     * @param stmt the assignment stmt
     */
    Alias(String name, AssignStmt stmt) {
        this.name = name;
        this.stmt = stmt;
    }

    /**
     * copy constructor for the Alias class.
     * @param a the Alias that is being copied
     */
    Alias(Alias a) {
        this.name = a.name;
        this.stmt = a.stmt;
    }

    /**
     * the the variable name
     * @return the variable name
     */
    public String get_name() {
        return name;
    }

    /**
     * get the variable assignment statement
     * @return the variable assignment statement
     */
    public AssignStmt get_stmt() {
        return stmt;
    }
}
