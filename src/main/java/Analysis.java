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

/**
 * The "Main" class for the Analysis of Bodies
 */
public class Analysis extends BodyTransformer {
	// Map that represents the current array versions that I block presents to a successor block
	private Map<Block, DownwardExposedArrayRef> c_arr_ver; // current array version
	// A worklist containing the block left to process
	private LinkedList<Block> worklist;
	// a list of blocks that have been seen
	private Set<Block> seen_blocks;
	// a list of blocks that have been seen IN THE CURRENT BFS execution
	// This is a bit more complex, these blocks are removed when the second iteration is parsed
	// Then they are added back.
	private Set<Block> loop_blocks;
	// A Set containing pairs of loop heads and exits (this will be important for nested loops)
	private Set<ImmutablePair<String, String>> loop_head_exits;
	// A Map that maps an array to an array version
	private Map<String, ArrayVersion> array_vars;
	// A list of variables that have been defined in the second iteration, this is needed so we do not
	// confuse mark a variable as having an intra-loop dependency when it was defined earlier in the
	// loop (it is contained in array_vars but has been defined)
	private Set<String> second_iter_def_vars;
	// The container class for all array ssa phi variables
	private PhiVariableContainer phi_vars;
	// A set of possible constants gathered from non-loop blocks
	private Set<String> constants;
	// A list of the _original_ phi variables that is queried on the second iteration
	private Set<String> top_phi_var_names;
	// The final DefUse Graph
	private ArrayDefUseGraph graph;
	// Dot graphs (for printing)
	private MutableGraph final_graph;
	private MutableGraph flow_graph;
	// server information
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
		top_phi_var_names = new HashSet<>();
		second_iter_def_vars = new HashSet<>();
		graph = new ArrayDefUseGraph();
		loop_blocks = new HashSet<>();
		constants = new HashSet<>();
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
			final_graph.add(def_node.link(to(use_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
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
			Unit u = i.next();
			ArrayVariableVisitor visitor = new ArrayVariableVisitor(array_vars, graph, Utils.get_block_num(b));
			u.apply(visitor);
			boolean is_array = visitor.get_is_array();
			VariableVisitor var_visitor =
					new VariableVisitor(phi_vars, top_phi_var_names, constants, is_array, false);
			u.apply(var_visitor);
			array_vars = visitor.get_vars();
			graph = visitor.get_graph();
			phi_vars = var_visitor.get_phi_vars();
			top_phi_var_names = var_visitor.get_top_phi_var_names();
			constants = var_visitor.get_constants();
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

	/**
	 * check the c_arr_ver map to see if all predecessor blocks have been visited
	 * @param preds_list the list of predecessor blocks
	 * @return true iff all predecessor blocks have been visited
	 */
	private boolean check_c_arr_ver(List<Block> preds_list) {
		Set<Block> possible = c_arr_ver.keySet();
		Set<Block> preds_set = new HashSet<>(preds_list);
		return possible.containsAll(preds_set);
	}

	/**
	 * check to see if all the array versions in a list of array versions are the same
	 * @param avs the list of ArrayVersions
	 * @return true iff all of the ArrayVersions have the same Version and the same Block number
	 */
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

	/**
	 * Process a Non-Merge block (BFS algo)
	 * @param b the Block
	 * @param pred the Predecessor block
	 * @param exits all exits for the loop
	 */
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

	/**
	 * Handle a block that is a merge of two or more predecessor blocks (BFS algo).
	 * @param b the Block
	 * @param exits all exits for the loop
	 */
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
					graph.add_node(n, true, false);
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

	/**
	 * Process a block in the BFS algorithm.
	 * This can be the first or the second iterations.
	 * @param b the Block
	 * @param head the head Block of the current loop
	 * @param exits all exits from the loop
	 * @param second_iter true iff we are parsing the loop for the second time.
	 */
	@SuppressWarnings({"ForLoopReplaceableByForEach", "DuplicatedCode"})
	private void process(Block b, Block head, List<String> exits, boolean second_iter) {
		List<Block> pred_blocks = b.getPreds();
		if(second_iter) {
			if (seen_blocks.contains(b)) {
				Logger.error("Were have seen this block on the second iter: " + b.getHead().toString());
				return;
			}
			for (Iterator<Unit> i = b.iterator(); i.hasNext(); ) {
				Unit u = i.next();
				IndexVisitor iv = new IndexVisitor(phi_vars, second_iter_def_vars,
						top_phi_var_names, constants, graph);
				u.apply(iv);
				second_iter_def_vars = iv.get_second_iter_def_vars();
				graph = iv.get_graph();
			}
			seen_blocks.add(b);
			worklist.addAll(b.getSuccs());
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
				Unit u = i.next();
				ArrayVariableVisitor av_visitor = new ArrayVariableVisitor(array_vars, graph, Utils.get_block_num(b));
				u.apply(av_visitor);
				boolean is_array = av_visitor.get_is_array();
				VariableVisitor var_visitor =
						new VariableVisitor(phi_vars, top_phi_var_names, constants, is_array, true);
				u.apply(bfs_visitor);
				u.apply(var_visitor);
				array_vars = av_visitor.get_vars();
				c_arr_ver = bfs_visitor.get_c_arr_ver();
				graph = bfs_visitor.get_graph();
				phi_vars = var_visitor.get_phi_vars();
				top_phi_var_names = var_visitor.get_top_phi_var_names();
				constants = var_visitor.get_constants();
			}
			List<Block> succ_blocks = b.getSuccs();
			Logger.debug("We found " + succ_blocks.size() + " successor blocks.");
			if (loop_head_exits.contains(new ImmutablePair<>(head.toString(), b.toString()))) {
				// TODO: this does nothing.
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

	/**
	 * Initialize all variable for the BFS algorithm
	 * @param b the Block
	 */
	private void init_BFS_vars(Block b) {
		DownwardExposedArrayRef down_ar = new DownwardExposedArrayRef(b);
		for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
			down_ar.put(entry.getKey(), Utils.copy_av(entry.getValue()));
		}
		c_arr_ver.put(b, down_ar);
	}

	/**
	 * Initialize the worklist for the BFS algorithm. The workslist represents
	 * the Blocks that are _left_ to parse
	 * @param head the Head Block of the Current loop
	 * @param second_iter true iff we are parsing the loop for the second time.
	 */
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

	/**
	 * perform a full parse of the loop (first or second iteration) for the BFS algorithm
	 * @param head the head of the currently loop
	 * @param exits all exits for the current loop
	 * @param second_iter true iff we are parsing the loop for the second time.
	 */
	private void parse_iteration(Block head, List<String> exits, boolean second_iter) {
		init_worklist(head, second_iter);
		// first iter
		while(!worklist.isEmpty()) {
			Block b = worklist.remove();
			process(b, head, exits, second_iter);
		}
		Logger.info("Finished " + second_iter);
	}

	/**
	 *  Perform the BFS algorithm on a loop
	 * @param head the Head of the loop
	 * @param exits all exits the current loops
	 */
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
		worklist.add(head);
		Logger.info("seen_blocks size 1: " + seen_blocks.size());
		seen_blocks.removeAll(loop_blocks);
		Logger.info("seen_blocks size 2: " + seen_blocks.size());
		parse_iteration(head, exits, true);
		seen_blocks.addAll(loop_blocks);
	}

	/**
	 * Overridden Soot method that parsed Code Bodies
	 * @param body the Current Code body
	 * @param phaseName the name of the Current Phase
	 * @param options The soot options
	 */
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
