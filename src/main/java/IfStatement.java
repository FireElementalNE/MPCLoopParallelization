import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * container class for an If statement
 */
class IfStatement {
    /**
     * the if statement
     */
    private final IfStmt stmt;
    /**
     * a list of successors
     */
    private final List<Unit> successors;

    /**
     * constructor
     * @param stmt the if statement
     * @param successors a list of successors
     */
    IfStatement(IfStmt stmt, List<Unit> successors) {
        this.stmt = stmt;
        this.successors = successors;
    }

    /**
     * getter for the unit that starts the TRUE branch
     * @return the unit that starts the TRUE branch
     */
    Unit get_true_branch() {
        Unit t = stmt.getTargetBox().getUnit();
        if(successors.contains(t)) {
            return t;
        }
        return null;
    }

    /**
     * getter for the unit that starts the TRUE branch
     * @return the unit that starts the TRUE branch
     */
    Unit get_false_branch() {
        return successors.stream()
                .filter(el -> !Objects.equals(get_true_branch(), el))
                .collect(Collectors.toList()).get(0);
    }

    /**
     * getter for the condition
     * @return the condition as a Value
     */
    @SuppressWarnings("unused")
    Value get_cond() {
        return stmt.getCondition();
    }

    /**
     * getter for the IfStmt
     * @return the IfStmt
     */
    IfStmt get_stmt() {
        return stmt;
    }
}
