import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.jimple.AssignStmt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class that solves constraints via SMT
 */
public class Solver implements Runnable{

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

    private ImmutablePair<Variable, List<AssignStmt>> dep_chain;
    private String index_name;
    private PhiVariableContainer phi_vars;
    private List<String> results;
    private String resolved_eq;
    private Map<String, Integer> constants;

    Solver(String index_name, ImmutablePair<Variable, List<AssignStmt>> dep_chain,
           PhiVariableContainer phi_vars, Map<String, Integer> constants) {
        this.index_name = index_name;
        this.dep_chain = dep_chain;
        this.phi_vars = phi_vars;
        this.resolved_eq = resolve_dep_chain();
        this.constants = constants;
    }

    int solve() {
        int d = 0;
        String resolved_eq = resolve_dep_chain();
        String filename = Constants.Z3_DIR + File.separator + "solver_z3_test_" + index_name + ".py";
        Path path = Paths.get(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            String res_eq_flat = resolved_eq.replace(" ", "");
            res_eq_flat = res_eq_flat.replace("$", "");
            writer.write("from z3 import *\n");
            writer.write("# " + resolved_eq + "\n");
            String left = res_eq_flat.split("=")[0];
            String rem = res_eq_flat.split("=")[1];
            // TODO: more split options
            Set<String> vars = new HashSet<>(Arrays.asList(rem.split("\\+|-|/|\\*|\\^|\\||%|&|~|>>|<<|>>>")));
            List<String> zero_neg_list = new ArrayList<>();
            writer.write(String.format("%s = Int('%s')\n", left, left));
            zero_neg_list.add(String.format(Constants.ZERO_TEST_PY_STR_NEG, left));
            for (String v : vars) {
                if (!NumberUtils.isCreatable(v)) {
                    writer.write(String.format("%s = Int('%s')\n", v, v));
                    if(constants.containsKey(v)) {
                        zero_neg_list.add(String.format(Constants.CONSTANTS_PY_STR, v, constants.get(v)));
                    } else {
                        zero_neg_list.add(String.format(Constants.ZERO_TEST_PY_STR_NEG, v));
                    }
                }
            }
            String zero_neg_str = String.join(", ", zero_neg_list);
            writer.write("F = [" + zero_neg_str + "]\n");
            writer.write("s = Solver()\n");
            writer.write("s.add(F)\n");
            writer.write(String.format("s.add(%s)\n", resolved_eq.replace("$", "").replace("=", "==")));
            writer.write("print(s.check())\n");
//            writer.write("print(s.model())\n");
            writer.write("if s.check() == z3.sat:\n");
            writer.write("    m = s.model()\n");
            writer.write("    for el in m:\n");
            writer.write("        print(el, m[el])\n");
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
                                lhs.getRight() - phi_val.getRight()));
                        d = lhs.getRight() - phi_val.getRight();
                    }
                } else {
                    Logger.error("What? We never set lhs.");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return d;

    }

    @SuppressWarnings("ConstantConditions")
    String resolve_dep_chain() {
        LinkedList<AssignStmt> stmts = new LinkedList<>(dep_chain.getRight());
        String base_stmt = null;
        for(int i = 0; i < stmts.size(); i++) {
            String left = stmts.get(i).getLeftOp().toString();
            if(Objects.equals(left, index_name)) {
                base_stmt = stmts.remove(i).toString();
                break;
            }
        }
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
        } else {
            base_stmt = index_name;
        }
        return base_stmt;
    }

    String get_resolved_eq() {
        return resolved_eq;
    }

    @Override
    public void run() {

    }
}
