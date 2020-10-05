import org.tinylog.Logger;
import soot.Body;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * class to find shimple line numbers of a statement in a body
 */
class BodyLineFinder {
    /**
     * the body
     */
    Body body;
    /**
     * a map connecting lines with line numbers
     */
    Map<String, Integer> line_map;

    /**
     * @param body the body
     */
    BodyLineFinder(Body body) {
        this.body = body;
        line_map = new HashMap<>();
        String[] lines = body.toString().split("\n");
        for(int i = 0; i < lines.length; i++) {
            Matcher m = Constants.LINE_PATTERN.matcher(lines[i]);
            if(m.matches()) {
                line_map.put(m.group(1), i);
            } else {
                Logger.debug("Found a non matching line: " + lines[i]);
            }

        }
    }

    /**
     * get the line number of a stmt as a string
     * @param s the statement as a string
     * @return the line number or -1 if it does not exist
     */
    int get_line(String s) {
        return line_map.getOrDefault(s, -1);
    }

    /**
     * get the line number of a stmt
     * @param s the stmt
     * @return the line number of -1 if it does not exist
     */
    int get_line(Stmt s) {
        return line_map.getOrDefault(s.toString(), -1);
    }
}
