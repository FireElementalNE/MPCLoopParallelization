import org.pmw.tinylog.Logger;
import soot.Body;
import soot.BodyTransformer;
import soot.jimple.ArrayRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.shimple.ShimpleBody;

import java.util.*;


public class Analysis extends BodyTransformer {

	private ArraySSAPhi get_phi(Map<String, ArraySSAPhi> phis, Stmt stmt) {
		ArrayRef array_ref = stmt.getArrayRef();
		String base_name = array_ref.getBaseBox().getValue().toString();
		for (Map.Entry<String, ArraySSAPhi> entry : phis.entrySet()) {
			if (Objects.equals(entry.getValue().get_base_name(), base_name)) {
				return entry.getValue();
			}
		}
		return null;
	}


	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		assert body instanceof ShimpleBody;
		LoopFinder lf = new LoopFinder();
		Set<Loop> loops = lf.getLoops(body);
		ArrayDefUseGraph graph = new ArrayDefUseGraph();
		Map<String, ArraySSAPhi> final_phis = new HashMap<>();
		for (Loop l : loops) {
			ArraySSAVisitor visitor = new ArraySSAVisitor(final_phis, graph);
			Logger.debug("Found a loop with head: " + l.getHead().toString());
			List<Stmt> stmts = l.getLoopStatements();
			List<String> loop_string = new ArrayList<>();
			for (Stmt s : stmts) {
				loop_string.add("  " + s.toString());
				s.apply(visitor);
				final_phis = visitor.get_phis();
			}
			// TODO: this is horrible, I will redo it.
			for(Map.Entry<Integer, Edge> entry : graph.get_edges().entrySet()) {
				for(Stmt s : stmts) {
					if(s.containsArrayRef()) {
						if(Objects.equals(entry.getValue().get_def().get_stmt().toString(), s.toString())) {
							ArraySSAPhi phi = get_phi(final_phis, s);
							if(!Objects.equals(phi, null)) {
								entry.getValue().get_def().set_phi(phi);
							}
						} else if(Objects.equals(entry.getValue().get_use().get_stmt().toString(), s.toString())) {
							ArraySSAPhi phi = get_phi(final_phis, s);
							if(!Objects.equals(phi, null)) {
								entry.getValue().get_def().set_phi(phi);
							}
						}
					}
				}
			}
			Logger.info("Node Count: " + graph.get_nodes().size());
			Logger.info("Edge Count: " + graph.get_edges().keySet().size());
			Logger.debug("Full Loop Body:");
			for (String s : loop_string) {
				Logger.debug(s);
			}
		}
		for (Map.Entry<String, ArraySSAPhi> ie : final_phis.entrySet()) {
			Logger.info(ie.getValue().get_phi_str());
		}
	}
}
