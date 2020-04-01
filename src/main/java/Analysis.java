import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.Body;
import soot.BodyTransformer;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.shimple.ShimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;

import java.io.IOException;
import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;


public class Analysis extends BodyTransformer {
	private Map<Block, DownwardExposedArrayRef> c_arr_ver; // current array version
	private LinkedList<Block> worklist;
	private Set<Block> seen_blocks;
	private Set<Block> loop_blocks;
	private Set<ImmutablePair<String, String>> loop_head_exits;
	private Map<String, ArrayVersion> array_vars;
	private PhiVariableContainer phi_vars;
	private ArrayDefUseGraph graph;
	private MutableGraph final_graph;
	private MutableGraph flow_graph;
	private int port;
	private String host;
	// TODO: finish documenting.

	/**
	 * Create an analysis object
	 * @param class_name the class that is being analyzed
	 */
	public Analysis(String class_name, String host, int port) {
		final_graph = mutGraph(class_name + "_final").setDirected(true);
		flow_graph = mutGraph(class_name + "_flow").setDirected(true);
		seen_blocks = new HashSet<>();
		c_arr_ver = new HashMap<>();
		worklist = new LinkedList<>();
		phi_vars = new PhiVariableContainer();
		loop_head_exits = new HashSet<>();
		array_vars = new HashMap<>();
		graph = new ArrayDefUseGraph();
		loop_blocks = new HashSet<>();
		this.host = host;
		this.port = port;
	}

	/**
	 * Make a pretty graph of the defuse graph
	 */
	private void make_graph_png() {
		for(Map.Entry<Integer, Edge> entry: graph.get_edges().entrySet()) {
			Edge e = entry.getValue();
			Node def = e.get_def();
			Node use = e.get_use();
			guru.nidi.graphviz.model.Node def_node = node(def.get_stmt());
			guru.nidi.graphviz.model.Node use_node = node(use.get_stmt());
			final_graph.add(def_node.link(to(use_node).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
		}
		Utils.print_graph(final_graph);
	}

	/**
	 * check is a given unit is the head of a loop
	 * @param unit the unit
	 * @return true iff the unit is the head of a loop
	 */
	private boolean is_loop_head(Unit unit) {
		Stmt stmt = (Stmt)unit;
		for(ImmutablePair<String, String> el : loop_head_exits) {
			if(Objects.equals(stmt.toString(), el.getKey())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add an edge for the flow graph (generated with GraphViz)
	 * @param from_blk the source block
	 * @param to_blk the target block
	 * @param is_loop true iff it is a looping edge (return to head)
	 */
	@SuppressWarnings("ConstantConditions")
	private void add_flow_edge(Block from_blk, Block to_blk, boolean is_loop, boolean second_iter) {
		if(!second_iter) {
			String from_name = Utils.get_block_name(from_blk);
			String to_name = Utils.get_block_name(to_blk);
			if(Utils.not_null(from_name) && Utils.not_null(to_name)) {
				guru.nidi.graphviz.model.Node from_node = node(from_name);
				guru.nidi.graphviz.model.Node to_node = node(to_name);
				if (is_loop) {
					flow_graph.add(from_node.link(to(to_node).with(Style.DASHED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
				} else {
					flow_graph.add(from_node.link(to(to_node).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
				}
			} else {
				Logger.error("Graph broke because RE broke.");
			}
		}
	}

	/**
	 * get all exits that correspond to a given unit
	 * @param unit the unit
	 * @return a list of exits that correspond to the unit
	 */
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

	/**
	 * Recursive block traversal function. This is the main parser for the _entire_ program.
	 * At this point we are _NOT_ in a loop
	 * @param b the block we are currently parsing
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void parse_block(Block b) {
		Logger.debug(Utils.get_block_name(b) + " head: " + b.getHead().toString());
		for(Iterator<Unit> i = b.iterator(); i.hasNext();) {
			ArrayVariableVisitor visitor = new ArrayVariableVisitor(array_vars, graph, Utils.get_block_num(b));
			VariableVisitor var_visitor = new VariableVisitor(phi_vars);
			Unit u = i.next();
			u.apply(visitor);
			u.apply(var_visitor);
			this.array_vars = visitor.get_vars();
			this.graph = visitor.get_graph();
			this.phi_vars = var_visitor.get_phi_vars();
		}
		if(is_loop_head(b.getHead())) {
			if(!seen_blocks.contains(b)) {
				Logger.info("We found a loop head, starting BFS: " + b.getHead());
				BFS(b, get_exits(b.getHead()));
			}
		}
		for(Block sb : b.getSuccs()) {
			if(!seen_blocks.contains(sb)) {
				add_flow_edge(b, sb, false, false);
				seen_blocks.add(b);
				parse_block(sb);
			}
		}
	}

	/**
	 * the start function for parsing blocks
	 * @param body the body being parsed
	 */
	private void parse_blocks_start(Body body) {
		BlockGraph bg = new ExceptionalBlockGraph(body);
		List<Block> blocks = bg.getHeads();
		for(Block b : blocks) {
			parse_block(b);
		}
	}

	/**
	 * find all loop heads/exits in a given body
	 * @param body the body being parsed
	 */
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

	private boolean check_c_arr_ver(List<Block> preds_list) {
		Set<Block> possible = c_arr_ver.keySet();
		Set<Block> preds_set = new HashSet<>(preds_list);
		return possible.containsAll(preds_set);
	}

	private boolean check_avs_diffs(List<ArrayVersion> avs) {
		boolean are_the_same = true;
		int version = avs.get(0).get_version();
		int block_num = avs.get(0).get_block();
		for(int i = 1; i < avs.size(); i++) {
			are_the_same = are_the_same
					&& ((version == avs.get(i).get_version())
					&& (block_num == avs.get(i).get_block()));
		}
		return are_the_same;
	}

	private void handle_non_merge(Block b, Block pred, List<String> exits) {
		if (!exits.contains(pred.getHead().toString())) {
			DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
			if (c_arr_ver.containsKey(pred)) {
				for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
					ArrayVersion current_s = Utils.copy_av(c_arr_ver.get(pred).get(entry.getKey()));
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

	private void handle_merge(Block b, List<String> exits) {
		List<Block> pred_blocks = b.getPreds();
		for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
			if (Utils.all_not_null(pred_blocks)) {
				List<ArrayVersion> avs = new ArrayList<>();
				for(Block blk : pred_blocks) {
					DownwardExposedArrayRef daf = new DownwardExposedArrayRef(c_arr_ver.get(blk));
					avs.add(Utils.copy_av(daf.get(entry.getKey())));
				}
				// make a phi node!
				// Indexes MUST be the same!
				// ONLY make a phi node if the versions are ACTUALLY different
				if(!check_avs_diffs(avs)) {
					ArrayVersionPhi av_phi = new ArrayVersionPhi(avs, Utils.get_block_num(b));
					DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
					// TODO: branch renaming???? if we branch just call new vars a or b then for further branches aa, bb etc??
					new_daf.put(entry.getKey(), av_phi);
					Logger.info("We made a phi node: " + new_daf.get_name(entry.getKey()));
					Node n = new Node(entry.getKey(), av_phi);
					graph.add_node(n, true);
					c_arr_ver.put(b, new_daf);
				} else {
					Logger.info("Branching changed nothing, not creating phi node.");
					for(Block pred : pred_blocks) {
						handle_non_merge(b, pred, exits);
					}
				}
			} else {
				Logger.info("False phi found!");
				DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
				c_arr_ver.put(b, new_daf);
			}
		}
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void process(Block b, Block head, List<String> exits, boolean second_iter) {
		List<Block> pred_blocks = b.getPreds();
		if(second_iter) {
			for (Iterator<Unit> i = b.iterator(); i.hasNext(); ) {
				Unit u = i.next();
				IndexVisitor iv = new IndexVisitor(phi_vars);
				u.apply(iv);
			}
			// TODO: remove this if stmt and replace with true second iteration:
			//       look at current index variables and how they have changed!
		} else {
			if (seen_blocks.contains(b)) {
				Logger.error("Were have seen this block: " + b.getHead().toString());
				return;
			}
			// TODO: update indexes!
			Logger.info(Utils.get_block_name(b) + ": " + b.getHead().toString());
			if (b.getPreds().size() > 1) { // we have a merge
				if (check_c_arr_ver(b.getPreds())) {
					handle_merge(b, exits);
				} else {
					Logger.info("Both preds should already be in c_arr_ver!");
					Logger.info("Skipping and adding this block to the back of worklist.");
					worklist.addLast(b);
					return;
				}
				handle_merge(b, exits);
			} else { // we do not have a merge
				// second iter is always false here.
				handle_non_merge(b, pred_blocks.get(0), exits);
			}
			// process stmts
			for (Iterator<Unit> i = b.iterator(); i.hasNext(); ) {
				BFSVisitor bfs_visitor = new BFSVisitor(c_arr_ver, b, graph, Utils.get_block_num(b));
				ArrayVariableVisitor av_visitor = new ArrayVariableVisitor(array_vars, graph, Utils.get_block_num(b));
				VariableVisitor var_visitor = new VariableVisitor(phi_vars);
				Unit u = i.next();
				u.apply(bfs_visitor);
				u.apply(av_visitor);
				u.apply(var_visitor);
				array_vars = av_visitor.get_vars();
				c_arr_ver = bfs_visitor.get_c_arr_ver();
				graph = bfs_visitor.get_graph();
				phi_vars = var_visitor.get_phi_vars();
			}
			List<Block> succ_blocks = b.getSuccs();
			Logger.debug("We found " + succ_blocks.size() + " successor blocks.");
			if (loop_head_exits.contains(new ImmutablePair<>(head.toString(), b.toString()))) {
				Logger.info("We found an exit, stopping.");
			} else {
				for (Block s1 : succ_blocks) {
					if (!seen_blocks.contains(s1)) {
						add_flow_edge(b, s1, false, false);
						worklist.addLast(s1);
					} else if (Objects.equals(Utils.get_block_name(s1), Utils.get_block_name(head))) {
						Logger.info("We found the head!");
						add_flow_edge(b, head, true, false);
						// TODO: should I clear the worklist?
					}
				}
			}
			seen_blocks.add(b);
			loop_blocks.add(b);
		}
	}

	private void init_BFS_vars(Block b) {
		DownwardExposedArrayRef down_ar = new DownwardExposedArrayRef(b);
		for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
			down_ar.put(entry.getKey(), Utils.copy_av(entry.getValue()));
		}
		c_arr_ver.put(b, down_ar);
	}

	private void init_worklist(Block head, boolean second_iter) {
		for(Block b : head.getSuccs()) {
			String head_str = head.getHead().toString();
			String exit_str = b.getHead().toString();
			ImmutablePair<String, String> head_exit = new ImmutablePair<>(head_str, exit_str);
			if(loop_head_exits.contains(head_exit)) {
				Logger.debug("Found an exit, skipping.");
			} else {
				add_flow_edge(head, b, false, second_iter);
				worklist.addFirst(b);
			}
		}
	}

	private void parse_iteration(Block head, List<String> exits, boolean second_iter) {
		init_worklist(head, second_iter);
		// first iter
		while(!worklist.isEmpty()) {
			Block b = worklist.remove();
			process(b, head, exits, second_iter);
		}
		Logger.info("Finished " + second_iter);
	}

	private void BFS(Block head, List<String> exits) {
		Logger.info("seen_blocks size 0: " + seen_blocks.size());
		seen_blocks.add(head);
		loop_blocks.add(head);
		init_BFS_vars(head);
		// CHECKTHIS: Assuming we only have one head...
		parse_iteration(head, exits, false);
		// second iter
		Logger.info("Entering second iteration!");
		// Empty worklist
		worklist = new LinkedList<>();
		Logger.info("seen_blocks size 1: " + seen_blocks.size());
		seen_blocks.removeAll(loop_blocks);
		Logger.info("seen_blocks size 2: " + seen_blocks.size());
		parse_iteration(head, exits, true);
		seen_blocks.addAll(loop_blocks);
	}

	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		if(!Constants.JUST_COMPILE) {
			assert body instanceof ShimpleBody;
			for (Map.Entry<String, String> e : options.entrySet()) {
				Logger.debug(e.getKey() + " ----> " + e.getValue());
			}
			find_loop_heads(body);
			parse_blocks_start(body);

			Logger.info("Node count: " + graph.get_nodes().size());
			for (Map.Entry<String, Node> entry : graph.get_nodes().entrySet()) {
				Logger.info(entry.getKey() + " -> " + entry.getValue().get_stmt());
			}
			Logger.info("Edge count: " + graph.get_edges().size());
			for (Map.Entry<Integer, Edge> entry : graph.get_edges().entrySet()) {
				Logger.info(entry.getKey() + ": ");
				Logger.info(" " + entry.getValue().get_def().get_stmt());
				Logger.info(" " + entry.getValue().get_use().get_stmt());
			}
			make_graph_png();
			Utils.print_graph(flow_graph);
			phi_vars.make_graphs();
			List<PhiVariable> linked_pvars = phi_vars.get_looping_index_vars();
			for (PhiVariable pv : linked_pvars) {
				ValueBox var = pv.get_phi_def();
				Logger.info("Checking def for looping index var: " + var.getValue().toString());
				List<AssignStmt> assignments = pv.get_linked_stmts();
				phi_vars.print_var_dep_chain(var.getValue().toString());
				Logger.debug("Here are the linked assignment stmts: ");
				for (AssignStmt stmt : assignments) {
					String linked_var = stmt.getLeftOpBox().getValue().toString();
					try {
						Solver solver = new Solver(stmt, host, port);
						String resp = solver.send_recv_stmt();
						Logger.debug("Got this from server: " + resp);

					} catch (IOException e) {
						Logger.error("Could not send '" + stmt.toString() + "' to solver.");
					}
					phi_vars.print_var_dep_chain(linked_var);
				}

			}
		}
	}
}
