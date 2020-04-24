import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
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
    private PhiVariableContainer phi_vars;
    private List<String> results;

    Solver(String index_name, ImmutablePair<Variable, List<AssignStmt>> dep_chain,
           PhiVariableContainer phi_vars, String host, int port) {
        this.host = host;
        this.port = port;
        this.index_name = index_name;
        this.dep_chain = dep_chain;
        this.phi_vars = phi_vars;
        this.resoved_eq = resolver();
    }

    void solve() {
        String filename = Constants.Z3_DIR + File.separator + "solver_z3_test_" + index_name + ".py";
        Path path = Paths.get(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            String res_eq_flat = resoved_eq.replace(" ", "");
            res_eq_flat = res_eq_flat.replace("$", "");
            writer.write("from z3 import *\n");
            writer.write("# " + resoved_eq + "\n");
            String left = res_eq_flat.split("=")[0];
            String rem = res_eq_flat.split("=")[1];
            // TODO: more split options
            Set<String> vars = new HashSet<>(Arrays.asList(rem.split("\\+|-|/|\\*|\\^|\\||%|&|~|>>|<<|>>>")));
            List<String> zero_neg_list = new ArrayList<>();
            writer.write(String.format("%s = Int('%s')\n", left, left));
            zero_neg_list.add(String.format(Constants.ZERO_TEST_PY_STR, left));
            for (String v : vars) {
                if (!NumberUtils.isCreatable(v)) {
                    writer.write(String.format("%s = Int('%s')\n", v, v));
                    zero_neg_list.add(String.format(Constants.ZERO_TEST_PY_STR, v));
                }
            }
            String zero_neg_str = String.join(", ", zero_neg_list);
            writer.write("F = [" + zero_neg_str + "]\n");
            writer.write("s = Solver()\n");
            writer.write("s.add(F)\n");
            writer.write(String.format("s.add(%s)\n", resoved_eq.replace("$", "").replace("=", "==")));
            writer.write("print(s.check())\n");
//            writer.write("print(s.model())\n");
            writer.write("m = s.model()\n");
            writer.write("for el in m:\n");
            writer.write("    print(el, m[el])\n");
            writer.close();
            // run command
            List<String> results = Utils.execute_cmd_ret(String.format(Constants.RUN_SOLVER_CMD, filename));
            List<ImmutablePair<String, Integer>> phi_var_vals = new ArrayList<>();
            ImmutablePair<String, Integer> lhs = null;
            boolean set_lhs = false;
            if (!Objects.equals(results.get(0), Constants.SAT)) {
                Logger.error("Z3 did not return sat: " + results.get(0));
                System.exit(0);
            } else {
                Logger.info("Z3 returned SAT.");
                for (int i = 1; i < results.size(); i++) {
                    String[] split_val = results.get(i).split(" ");
                    if (phi_vars.is_used_in_phi(split_val[0]) || phi_vars.is_phi_def(split_val[0])) {
                        phi_var_vals.add(new ImmutablePair<>(split_val[0], Integer.parseInt(split_val[1])));
                    } else if (!set_lhs && Objects.equals(split_val[0], left)) {
                        lhs = new ImmutablePair<>(split_val[0], Integer.parseInt(split_val[1]));
                        set_lhs = true;
                    }
                    // TODO: the following DOES NOT trigger in debug but triggers in RUN???
//                else {
//                    Logger.error("Error: found non phi, non lhs var -> " + results.get(i));
//                    System.exit(0);
//                }
                }
                if (set_lhs) {
                    for (ImmutablePair<String, Integer> phi_val : phi_var_vals) {
                        Logger.info(String.format("d value between %s and %s: %d", lhs.getLeft(), phi_val.getLeft(),
                                Math.abs(lhs.getRight() - phi_val.getRight())));
                    }
                } else {
                    Logger.error("What? We never set lhs.");
                    System.exit(0);
                }
            }
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
