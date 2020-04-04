import soot.jimple.AssignStmt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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

    Solver(AssignStmt stmt, String host, int port) throws IOException {
        this.stmt = stmt;
        this.host = host;
        this.port = port;
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
}
