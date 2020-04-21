import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import soot.jimple.AssignStmt;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class that solves constraints via SMT
 */
public class Solver {

    // TODO: this will be the solver class!

    /*
        I am going to use z3-solver python package (it is a nightmare in java....)
        1. Write the file
        2. run the program
        3. get the results!

        For example turn this:

            i3 (i14_1 - 1) -> i14_1 (Phi(i14, i14_2))

        Into this:

            from z3 import *
            i14_1 = Int('i14_1')
            i3 = Int('i3')
            s = Solver()
            s.add(i3 == i14_1 - 1)
            print(s.check())
            print(s.model())

     */

    private AssignStmt stmt;
    private String host;
    private int port;
    private ImmutablePair<Variable, List<AssignStmt>> dep_chain;
    private String index_name;
    private String resoved_eq;

    Solver(String index_name, ImmutablePair<Variable, List<AssignStmt>> dep_chain,
           Set<String> constants, String host, int port) {
        this.host = host;
        this.port = port;
        this.index_name = index_name;
        this.dep_chain = dep_chain;
        this.resoved_eq = resolver();
        write_solver();
    }

    Solver(AssignStmt stmt, String host, int port) throws IOException {
        this.stmt = stmt;
        this.host = host;
        this.port = port;
    }

    private void write_solver() {
        Path path = Paths.get("solver_z3_test_" + index_name + ".py");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            String res_eq_flat = resoved_eq.replace(" ", "");
            res_eq_flat = res_eq_flat.replace("$", "");
            writer.write("from z3 import *\n");
            writer.write("# "  + resoved_eq + "\n");
            String left = res_eq_flat.split("=")[0];
            String rem = res_eq_flat.split("=")[1];
            // TODO: more split options
            writer.write(String.format("%s = Int('%s')\n", left, left));
            Set<String> vars = new HashSet<>(Arrays.asList(rem.split("\\+|-|/|\\*|\\^|\\||%|&|~|>>|<<|>>>")));
            for(String v : vars) {
                if(!NumberUtils.isCreatable(v)) {
                    writer.write(String.format("%s = Int('%s')\n", v, v));
                }
            }
            writer.write("s = Solver()\n");
            writer.write( String.format("s.add(%s)\n", resoved_eq.replace("$", "").replace("=", "==")));
            writer.write("print(s.check())\n");
            writer.write("print(s.model())\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private String resolver() {
        LinkedList<AssignStmt> stmts = new LinkedList<>(dep_chain.getRight());
        String base_stmt = null;
        for(int i = 0; i < stmts.size(); i++) {
            String left = stmts.get(i).getLeftOp().toString();
            if(Objects.equals(left, index_name)) {
                base_stmt = stmts.remove(i).toString();
                break;
            }
        }
        assert base_stmt != null;
        if(Utils.not_null(base_stmt)) {
            while(!stmts.isEmpty()) {
                AssignStmt current_stmt = stmts.remove(0);
                String left = current_stmt.getLeftOp().toString();
                List<String> split_lst = Arrays.asList(base_stmt.split(" "));
                if(split_lst.contains(left)) {
                    String right = current_stmt.getRightOp().toString();
                    int index = split_lst.indexOf(left);
                    split_lst.set(index, right);
                    base_stmt = String.join(" ", split_lst);
                } else {
                    stmts.addLast(current_stmt);
                }
            }
        }
        return base_stmt;
    }

    String send_recv_stmt() throws IOException {
        Socket s = new Socket(host, port);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        byte[] bytes = stmt.toString().getBytes(StandardCharsets.UTF_8);
        out.write(bytes);
        String ans = br.readLine();
        s.close();
        return ans;
    }

    String get_resoved_eq() {
        return resoved_eq;
    }
}
