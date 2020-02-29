import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pmw.tinylog.Logger;
import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.shimple.ShimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;

import java.util.*;


public class Analysis extends BodyTransformer {
	private Map<Block, DownwardExposedArrayRef> c_arr_ver; // current array version
	private LinkedList<Block> worklist;
	private Set<Block> seen_blocks1;
	private Set<Block> seen_blocks2;
	private Set<ImmutablePair<String, String>> loop_head_exits;
	private Map<String, ArrayVersion> array_vars;
	private ArrayDefUseGraph graph;
	// TODO: we need to keep track of arrays that are reassigned otherwise we will
	// TODO:  never find the correct versions when we are looking in c_arr_ver.
	public Analysis() {
		seen_blocks1 = new HashSet<>();
		seen_blocks2 = new HashSet<>();
		c_arr_ver = new HashMap<>();
		worklist = new LinkedList<>();
		loop_head_exits = new HashSet<>();
		array_vars = new HashMap<>();
		graph = new ArrayDefUseGraph();
	}

	private boolean is_loop_head(Unit unit) {
		Stmt stmt = (Stmt)unit;
		for(ImmutablePair<String, String> el : loop_head_exits) {
			if(Objects.equals(stmt.toString(), el.getKey())) {
				return true;
			}
		}
		return false;
	}

	private List<String> get_exits(Unit unit) {
		List<String> exits = new ArrayList<>();
		Stmt stmt = (Stmt)unit;
		for(ImmutablePair<String, String> el : loop_head_exits) {
			if(Objects.equals(stmt.toString(), el.getKey())) {
				exits.add(el.getValue());
			}
		}
		return exits;
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void parse_block(Block b, int sc) {
		seen_blocks1.add(b);
		// Logger.info(Utils.fill_spaces(sc) + "Found Block (head is first line).");
		// Logger.info(Utils.fill_spaces(sc) + "Number of successor Blocks: " + b.getSuccs().size());
		// Logger.info(Utils.fill_spaces(sc) + "Preds size: " + b.getPreds().size());
		Logger.debug(Utils.get_block_name(b) + " head: " + b.getHead().toString());
		for(Iterator<Unit> i = b.iterator(); i.hasNext();) {
			ArrayVariableVisitor visitor = new ArrayVariableVisitor(array_vars, graph);
			i.next().apply(visitor);
			array_vars = visitor.get_vars();
			this.graph = visitor.get_graph();
		}
		if(is_loop_head(b.getHead())) {
			if(!seen_blocks2.contains(b)) {
				Logger.info("We found a loop head, starting BFS: " + b.getHead());
				BFS(b, get_exits(b.getHead()));
			}
		}
		for(Block sb : b.getSuccs()) {
			if(!seen_blocks1.contains(sb)) {
				parse_block(sb, sc + 1);
			}
		}
	}

	private void parse_blocks_start(Body body) {
		BlockGraph bg = new ExceptionalBlockGraph(body);
		List<Block> blocks = bg.getBlocks();
		for(Block b : blocks) {
			parse_block(b, 0);
		}
	}

	// New Ana Algo!
	private void find_loop_heads(Body body) {
		LoopFinder lf = new LoopFinder();
		Set<Loop> loops = lf.getLoops(body);
		for(Loop l : loops) {
			String head_str = l.getHead().toString();
			Collection<Stmt> exits = l.getLoopExits();
			for(Stmt exit : exits) {
				IfStmt real_exit = (IfStmt)exit;
				String exit_str = real_exit.getTarget().toString();
				Logger.debug("Head/Exit found => " + head_str + " ----> " + exit_str);
				loop_head_exits.add(new ImmutablePair<>(head_str, exit_str));
			}
		}
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void process(Block b, Block head, List<String> exits) {
		if(seen_blocks1.contains(b) || seen_blocks2.contains(b)) {
			Logger.error("Were have seen this block: " + b.getHead().toString());
			return;
		}
		seen_blocks1.add(b);
		seen_blocks2.add(b);
		Logger.info(Utils.get_block_name(b) + ": " + b.getHead().toString());
		List<Block> pred_blocks = b.getPreds();
		Block b1 = pred_blocks.get(0);
		if(pred_blocks.size() > 1) { // we have a merge
			// CHECKTHIS: we are assuming we only have 2 pred blocks!
			Block b2 = pred_blocks.get(1);
			if (c_arr_ver.containsKey(b1) && c_arr_ver.containsKey(b2)) {
				for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
					DownwardExposedArrayRef daf1 = new DownwardExposedArrayRef(c_arr_ver.get(b1));
					DownwardExposedArrayRef daf2 = new DownwardExposedArrayRef(c_arr_ver.get(b2));
					ArrayVersion current_s1 = daf1.get(entry.getKey());
					ArrayVersion current_s2 = daf2.get(entry.getKey());
					// TODO: check if phi nodes?
					if (Utils.not_null(b1) && Utils.not_null(b2)) {
						// make a phi node!
						ArrayVersion av = new ArrayVersion(current_s1.get_v1(), current_s2.get_v1());
						DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
						new_daf.put(entry.getKey(), av);
						Logger.info("We made a phi node: " + new_daf.get_name(entry.getKey()));
						Node n = new Node(entry.getKey(), av);
						graph.add_node(n, true);
						c_arr_ver.put(b, new_daf);
					} else {
						Logger.info("False phi found!");
						DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
						c_arr_ver.put(b, new_daf);
					}
				}
			} else {
				Logger.warn("Both should already be in c_arr_ver!");
				System.exit(0);
			}
		} else { // we do not have a merge
			if(!exits.contains(pred_blocks.get(0).getHead().toString())) {
				DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
				if (c_arr_ver.containsKey(b1)) {
					for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
						// CHECKTHIS: throwing NullPointerException because of aliasing issue (see top of class)
						ArrayVersion current_s = new ArrayVersion(c_arr_ver.get(b1).get(entry.getKey()));
						new_daf.put(entry.getKey(), current_s);
					}
					c_arr_ver.put(b, new_daf);

				} else {
					Logger.warn("This should already be c_arr_ver!");
				}
			} else {
				Logger.warn("Pred is an exit, skipping.");
			}
		}
		// process stmts
		for(Iterator<Unit> i = b.iterator(); i.hasNext();) {
			BFSVisitor bfs_visitor = new BFSVisitor(c_arr_ver, b, graph);
			ArrayVariableVisitor av_visitor = new ArrayVariableVisitor(array_vars, graph);
			Unit u = i.next();
			u.apply(bfs_visitor);
			u.apply(av_visitor);
			array_vars = av_visitor.get_vars();
			c_arr_ver = bfs_visitor.get_c_arr_ver();
			graph = bfs_visitor.get_graph();
		}
		List<Block> succ_blocks = b.getSuccs();
		Logger.debug("We found " + succ_blocks.size() + " successor blocks.");
		if(loop_head_exits.contains(new ImmutablePair<>(head.toString(), b.toString()))) {
			Logger.info("We found an exit, stopping.");
		} else {
			for(Block s1 : succ_blocks) {
				if (!seen_blocks2.contains(s1)) {
					worklist.addLast(s1);
				}
			}
		}
	}

	private void init_BFS_vars(Block b) {
		DownwardExposedArrayRef down_ar = new DownwardExposedArrayRef(b);
		for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
			down_ar.put(entry.getKey(), new ArrayVersion(1));
		}
		c_arr_ver.put(b, down_ar);
	}

	private void BFS(Block head, List<String> exits) {
		seen_blocks2.add(head);
		init_BFS_vars(head);
		// CHECKTHIS: Assuming we only have one head...
		for(Block b : head.getSuccs()) {
			String head_str = head.getHead().toString();
			String exit_str = b.getHead().toString();
			ImmutablePair<String, String> head_exit = new ImmutablePair<>(head_str, exit_str);
			if(loop_head_exits.contains(head_exit)) {
				Logger.debug("Found an exit, skipping.");
			} else {
				worklist.addFirst(b);
			}
		}
		while(!worklist.isEmpty()) {
			Block b = worklist.remove();
			process(b, head, exits);
		}
	}

	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		assert body instanceof ShimpleBody;
		// ShimpleBody sb = (ShimpleBody)body;
		for(Map.Entry<String, String> e : options.entrySet()) {
			Logger.debug(e.getKey() + " ----> " + e.getValue());
		}
		find_loop_heads(body);
		parse_blocks_start(body);

		Logger.info("Node count: " + graph.get_nodes().size());
		Logger.info("Edge count: " + graph.get_edges().size());
	}
}
