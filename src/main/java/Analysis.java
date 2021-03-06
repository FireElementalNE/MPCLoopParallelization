import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.shimple.ShimpleBody;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

/**
 * The "Main" class for the Analysis of Bodies
 */
public class Analysis extends BodyTransformer {

	// No loop carried dependencies for this! the arrays are only read only or write only.
	// The only dependency that is carried is for the sum variable. Make a check, if an array
	// is not read and written to it means nothing.
	// Get loops from HPC literature
	// FIX: need to finish SCC by next week, a lot of work.

	/**
	 * Map that represents the current array versions that a block presents to a successor block
	 */
	private Map<Block, DownwardExposedArrayRef> c_arr_ver; // current array version
	/**
	 * A worklist containing the block left to process
	 */
	private LinkedList<Block> worklist;
	/**
	 * a list of blocks that have been seen
	 */
	final private Set<Block> seen_blocks;
	/**
	 * a list of blocks that have been seen IN THE CURRENT BFS execution
	 * This is a bit more complex, these blocks are removed when the second iteration is parsed
	 * Then they are added back.
	 */
	final private Set<Block> loop_blocks;
	/**
	 * A Set containing pairs of loop heads and exits (this will be important for nested loops)
	 */
	final private Set<ImmutablePair<String, String>> loop_head_exits;
	/**
	 * A wrapper class that contains a Map for all array variables to  array version
	 */
	private ArrayVariables array_vars;
	/**
	 * A list of variables that have been defined in the second iteration, this is needed so we do not
	 * confuse mark a variable as having an intra-loop dependency when it was defined earlier in the
	 * loop (it is contained in array_vars but has been defined)
	 */
	private Set<String> second_iter_def_vars;
	/**
	 * the container class that holds all non array phi variables
	 */
	private PhiVariableContainer phi_vars;
	/**
	 * A set of possible constants gathered from non-loop blocks
	 */
	private Map<String, Integer> constants;
	/**
	 * A list of the _original_ phi variables that is queried on the second iteration
	 */
	private Set<String> top_phi_var_names;
	/**
	 * The final DefUse Graph
	 */
	private ArrayDefUseGraph graph;
	/**
	 * the final SCC graph
	 */
	private SCCGraph scc_graph;
	/**
	 * Dot graphs (for printing)
	 */
	final private MutableGraph flow_graph;
	/**
	 * container for if statements (used in def/use graph)
	 */
	private IfStatementContainer if_stmts;
	/**
	 * condition stack
	 */
	private Stack<IfStmt>cond_stk;
	/**
	 * a list of all constructed array phis
	 */
	private final List<ImmutablePair<String, ArrayVersionPhi>> array_phis;
	/**
	 * set of new array statements
	 */
	private Set<Stmt> new_array_stmts;
	/**
	 * map to handle array USES being used in IFSTMTS (and therefore MUX stmts)
	 */
	private Map<String, ValueBox> array_reads_for_if_stmts;
	/**
	 * list of loops
	 */
	private Set<Loop> loops;
	/**
	 * Construct to find shimple lines
	 */
	BodyLineFinder blf;

	/**
	 * Create an analysis object
	 * @param class_name the class that is being analyzed
	 */
	public Analysis(String class_name) {
		flow_graph = mutGraph(class_name + "_flow").setDirected(true);
		seen_blocks = new HashSet<>();
		c_arr_ver = new HashMap<>();
		worklist = new LinkedList<>();
		phi_vars = new PhiVariableContainer(class_name);
		loop_head_exits = new HashSet<>();
		array_vars = new ArrayVariables();
		graph = new ArrayDefUseGraph(class_name);
		loop_blocks = new HashSet<>();
		constants = new HashMap<>();
		top_phi_var_names = new HashSet<>();
		second_iter_def_vars = new HashSet<>();
		scc_graph = new SCCGraph(class_name);
		if_stmts = new IfStatementContainer();
		cond_stk = new Stack<>();
		array_phis = new ArrayList<>();
		new_array_stmts = new HashSet<>();
		array_reads_for_if_stmts = new HashMap<>();
		loops = new HashSet<>();
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
	 * @param second_iter true iff we are parsing on the second iteration
	 */
	@SuppressWarnings("ConstantConditions")
	private void add_flow_edge(Block from_blk, Block to_blk, boolean is_loop, boolean second_iter) {
		if(!second_iter) {
			String from_name = Utils.get_block_name(from_blk);
			String to_name = Utils.get_block_name(to_blk);
			if (Utils.not_null(from_name) && Utils.not_null(to_name)) {
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
	 * @param body the whole program body
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void parse_block(Block b, Body body) {
		Logger.debug(Utils.get_block_name(b) + " head: " + b.getHead().toString());
		boolean is_merge = false;
		if(b.getPreds().size() > 1) {
			is_merge = true;
		}
		for(Iterator<Unit> i = b.iterator(); i.hasNext();) {
			Unit u = i.next();
			ArrayVariableVisitor visitor = new ArrayVariableVisitor(array_vars,
					graph, Utils.get_block_num(b), new_array_stmts, blf);
			u.apply(visitor);
			new_array_stmts = visitor.get_new_array_stmts();
			boolean is_array = visitor.get_is_array();
			VariableVisitor var_visitor =
					new VariableVisitor(phi_vars, top_phi_var_names, constants, is_array, false,
							is_merge, new_array_stmts, array_reads_for_if_stmts);
			u.apply(var_visitor);
			array_reads_for_if_stmts = var_visitor.get_array_reads_for_if_stmts();
			new_array_stmts = var_visitor.get_new_array_stmts();
			array_vars = visitor.get_vars();
			graph = visitor.get_graph();
			phi_vars = var_visitor.get_phi_vars();
			top_phi_var_names = var_visitor.get_top_phi_var_names();
			constants = var_visitor.get_constants();
		}
		if(is_loop_head(b.getHead())) {
			if(!seen_blocks.contains(b)) {
				Logger.info("We found a loop head, starting BFS: " + b.getHead());
				BFS(b, get_exits(b.getHead()), body);
			}
		}
		for(Block sb : b.getSuccs()) {
			if(!seen_blocks.contains(sb)) {
				add_flow_edge(b, sb, false, false);
				seen_blocks.add(b);
				parse_block(sb, body);
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
			parse_block(b, body);
		}
	}

	/**
	 * find all loop heads/exits current body
	 */
	private void find_loop_heads() {
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
				for (Map.Entry<String, ArrayVersion> entry : array_vars.entry_set()) {
					ArrayVersion current_s;
					if(c_arr_ver.containsKey(b) && c_arr_ver.get(b).contains_var(entry.getKey())) {
						current_s = c_arr_ver.get(b).get(entry.getKey());
					} else {
						current_s = Utils.copy_av(c_arr_ver.get(pred).get(entry.getKey()));
					}
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
		if(array_vars.entry_set().isEmpty()) {
			List<DownwardExposedArrayRef> dafs = new ArrayList<>();
			for(Block pred_b : pred_blocks) {
				dafs.add(c_arr_ver.get(pred_b));
			}
			boolean test = dafs.stream().map(DownwardExposedArrayRef::is_empty).collect(Collectors.toList())
								.stream().reduce(true, Boolean::logicalAnd);
			if(test) {
				c_arr_ver.put(b, new DownwardExposedArrayRef(b));
			} else {
				Logger.error("There should be an array version in one of the pred blocks!");
				System.exit(0);
			}
		} else {
			for (Map.Entry<String, ArrayVersion> entry : array_vars.entry_set()) {
				if (Utils.all_not_null(pred_blocks)) {
					List<ArrayVersion> avs = new ArrayList<>();
					for (Block blk : pred_blocks) {
						DownwardExposedArrayRef daf = new DownwardExposedArrayRef(c_arr_ver.get(blk));
						ArrayVersion new_av = Utils.copy_av(daf.get(entry.getKey()));
						if (!avs.stream().map(el -> el.get_version() == new_av.get_version()).reduce(false, Boolean::logicalOr)) {
							avs.add(new_av);
						}
					}
					// make a phi node!
					// Indexes MUST be the same!
					// ONLY make a phi node if the versions are ACTUALLY different
					if (!check_avs_diffs(avs)) {
						IfStmt ifstmt = cond_stk.pop();
						ArrayVersionPhi av_phi = new ArrayVersionPhi(avs, Utils.get_block_num(b), -1, ifstmt);
						DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
						// TODO: branch renaming???? if we branch just call new vars a or b then for further branches aa, bb etc??
						new_daf.put(entry.getKey(), av_phi);
						Logger.info("We made a phi node: " + new_daf.get_name(entry.getKey()));
						Logger.info("The if statement is: " + ifstmt.toString());
						array_phis.add(new ImmutablePair<>(entry.getKey(), av_phi));
						String resolved_dep_chain = ifstmt.toString();
						for(ValueBox vb : ifstmt.getCondition().getUseBoxes()) {
							if(!NumberUtils.isCreatable(vb.getValue().toString())) {
								ImmutablePair<Variable, List<AssignStmt>> dep_chain =
										phi_vars.get_var_dep_chain(constants, vb.getValue().toString());
								String eq = Utils.resolve_dep_chain(vb.getValue().toString(), dep_chain, constants);
								// TODO: fix the string so it only take the RHS of the eq
								resolved_dep_chain = resolved_dep_chain.replace(vb.getValue().toString(), eq);
							}
						}
						Logger.info("The resolved if statement is: " + resolved_dep_chain);

						Node n = new Node(entry.getKey(), av_phi, -1);
						graph.add_node(n, true, true);
						c_arr_ver.put(b, new_daf);
					} else {
						Logger.info("Branching changed nothing, not creating phi node.");
						for (Block pred : pred_blocks) {
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
	}

	/**
	 * Process a block in the BFS algorithm.
	 * This can be the first or the second iterations.
	 * @param b the Block
	 * @param head the head Block of the current loop
	 * @param exits all exits from the loop
	 * @param second_iter true iff we are parsing the loop for the second time.
	 */
	private void process(Block b, Block head, List<String> exits, boolean second_iter) {
		List<Block> pred_blocks = b.getPreds();
		if(second_iter) {
			if (seen_blocks.contains(b)) {
				Logger.error("Were have seen this block on the second iter: " + b.getHead().toString());
				return;
			}
			for (Unit u : b) {
				IndexVisitor iv = new IndexVisitor(phi_vars, second_iter_def_vars,
						top_phi_var_names, constants, scc_graph, blf);
				u.apply(iv);
				second_iter_def_vars = iv.get_second_iter_def_vars();
				scc_graph = iv.get_graph();
			}
			seen_blocks.add(b);
			worklist.addAll(b.getSuccs());
		} else {
			boolean is_merge = false;
			if (seen_blocks.contains(b)) {
				Logger.warn("Were have seen this block: " + b.getHead().toString());
				return;
			}
			Logger.info(Utils.get_block_name(b) + ": " + b.getHead().toString());
			if (b.getPreds().size() > 1) { // we have a merge
				if (check_c_arr_ver(b.getPreds())) {
					is_merge = true;
					handle_merge(b, exits);
				} else {
					Logger.info("Both preds should already be in c_arr_ver!");
					Logger.info("Skipping and adding this block to the back of worklist.");
					worklist.addLast(b);
					return;
				}
//				handle_merge(b, exits);
			} else { // we do not have a merge
				// second iter is always false here.
				handle_non_merge(b, pred_blocks.get(0), exits);
			}
			// process stmts
			for (Unit u : b) {
				ArrayVariableVisitor av_visitor = new ArrayVariableVisitor(array_vars,
						graph, Utils.get_block_num(b), new_array_stmts, blf);
				u.apply(av_visitor);
				array_vars = av_visitor.get_vars();
				new_array_stmts = av_visitor.get_new_array_stmts();
				BFSVisitor bfs_visitor = new BFSVisitor(c_arr_ver, b, graph,
						array_vars, phi_vars, constants, Utils.get_block_num(b), cond_stk, blf);
				u.apply(bfs_visitor);
				cond_stk = bfs_visitor.get_cond_stk();
				array_vars = bfs_visitor.get_vars();
				c_arr_ver = bfs_visitor.get_c_arr_ver();
				graph = bfs_visitor.get_graph();
				boolean is_array = av_visitor.get_is_array();
				VariableVisitor var_visitor =
						new VariableVisitor(phi_vars, top_phi_var_names, constants, is_array, true,
								is_merge, new_array_stmts, array_reads_for_if_stmts);
				u.apply(var_visitor);
				array_reads_for_if_stmts = var_visitor.get_array_reads_for_if_stmts();
				new_array_stmts = var_visitor.get_new_array_stmts();
				phi_vars = var_visitor.get_phi_vars();
				top_phi_var_names = var_visitor.get_top_phi_var_names();
				constants = var_visitor.get_constants();
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

	/**
	 * Initialize all variable for the BFS algorithm
	 * @param b the Block
	 */
	private void init_BFS_vars(Block b) {
		DownwardExposedArrayRef down_ar = new DownwardExposedArrayRef(b);
		for (Map.Entry<String, ArrayVersion> entry : array_vars.entry_set()) {
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
	 * parse the head for if statements to put on the condition stack
	 * @param head the head block
	 * @param b the whole program body
	 */
	void parse_head(Block head, Body b) {
		for(Unit u : head) {
			ExceptionalUnitGraph g = new ExceptionalUnitGraph(b);
			IfStatementVisitor if_v = new IfStatementVisitor(g, if_stmts, cond_stk);
			u.apply(if_v);
			cond_stk = if_v.get_cond_stk();
		}
		seen_blocks.add(head);
		loop_blocks.add(head);
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
	 * @param b the body of the program
	 */
	private void BFS(Block head, List<String> exits, Body b) {
		Logger.info("seen_blocks size 0: " + seen_blocks.size());
		parse_head(head, b);
		init_BFS_vars(head);
		// Assuming we only have one head...
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
//		for(ImmutablePair<String, ArrayVersionPhi> entry : array_phis) {
//			Logger.debug("Need to make a new phi node!");
//			List<ArrayVersion> versions = new ArrayList<>();
//			int block = 0;
//			int line_num = 0;
//			IfStmt if_stmt = null;
//			if(array_vars.contains_key(entry.getLeft())) {
//				versions.add(array_vars.get(entry.getLeft()));
//				block = array_vars.get(entry.getLeft()).get_block();
//				line_num = array_vars.get(entry.getLeft()).get_line_num();
//				find_loop_if_stmt(head);
//				// if_stmt = array_vars.get(entry.getLeft()).get
//
//			} else {
//				Logger.error("this is wrong!");
//			}
//			// TODO: have to handle renaming!
//			versions.add(entry.getRight());
//			ArrayVersionPhi av_phi = new ArrayVersionPhi(versions, block, line_num, null);
//			Logger.debug("New phi: " + Utils.create_phi_stmt(entry.getLeft(), av_phi));
//		}
	}
	// TODO: add javadoc to these class vars
	private MutableGraph g1;
	private List<Unit> seen;
	/**
	 * recursive helper for cfg graph creation
	 * @param g the graph
	 * @param c the current node
	 */
	void make_cfg_graph_helper(ExceptionalUnitGraph g, Unit c) {
		guru.nidi.graphviz.model.Node c_node = node(c.toString());
		IfStatementVisitor if_v = new IfStatementVisitor(g, if_stmts, cond_stk);
		c.apply(if_v);
		if_stmts = if_v.get_if_stmts();
		seen.add(c);
		for(Unit u : g.getSuccsOf(c)) {
			guru.nidi.graphviz.model.Node u_node = node(u.toString());
			g1.add(c_node.link(to(u_node).with(Style.ROUNDED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
			if(!seen.contains(u)) {
				make_cfg_graph_helper(g, u);
			}
		}
	}

	/**
	 * make a graph of the cfg
	 * @param b the body in question
	 */
	void make_cfg_graph(Body b) {
		ExceptionalUnitGraph g = new ExceptionalUnitGraph(b);
		g1 = mutGraph("test").setDirected(true);
		seen = new ArrayList<>();
		for(Unit u : g.getHeads()) {
			make_cfg_graph_helper(g, u);
		}
		Utils.print_graph(g1, "ff");
	}

	/**
	 * Overridden Soot method that parsed Code Bodies
	 * @param body the Current Code body
	 * @param phaseName the name of the Current Phase
	 * @param options The soot options
	 */
	@Override
	protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
		blf = new BodyLineFinder(body);
		make_cfg_graph(body);
		LoopFinder lf = new LoopFinder();
		loops = lf.getLoops(body);
		if(!Constants.JUST_COMPILE) {
			assert body instanceof ShimpleBody : "Has to be a shimple body.";
			for (Map.Entry<String, String> e : options.entrySet()) {
				Logger.debug(e.getKey() + " ----> " + e.getValue());
			}
			find_loop_heads();
			parse_blocks_start(body);
			Logger.info("Node count: " + graph.get_nodes().size());
			for (Map.Entry<String, Node> entry : graph.get_nodes().entrySet()) {
				Logger.info(entry.getKey() + " -> " + entry.getValue().get_aug_stmt_str());
			}
			Logger.info("Edge count: " + graph.get_edges().size());
			for (Map.Entry<Integer, Edge> entry : graph.get_edges().entrySet()) {
				Logger.info(entry.getKey() + ": ");
				Logger.info(" " + entry.getValue().get_def().get_aug_stmt_str());
				Logger.info(" " + entry.getValue().get_use().get_aug_stmt_str());
			}
			phi_vars.make_graphs();
			List<PhiVariable> linked_pvars = phi_vars.get_looping_index_vars();
			for (PhiVariable pv : linked_pvars) {
				Value var = pv.get_phi_def();
				Logger.info("Checking def for looping index var: " + var.toString());
				List<AssignStmt> assignments = pv.get_linked_stmts();
				phi_vars.print_var_dep_chain(constants, var.toString());
				Logger.debug("Here are the linked assignment stmts: ");
				for (AssignStmt stmt : assignments) {
					String linked_var = stmt.getLeftOpBox().getValue().toString();
					phi_vars.print_var_dep_chain(constants, linked_var);
				}
			}
			Utils.print_graph(flow_graph, Constants.EMPTY_FLOW_GRAPH);
			graph.make_graph(phi_vars, constants);
			scc_graph.make_scc_graph(phi_vars, constants, graph, if_stmts, array_vars);

			Set<SCCEdge> edges = scc_graph.get_edges();
			Logger.debug("Edge count: " + edges.size());
			for(SCCEdge e : edges) {
				Logger.debug("\tEDGE: " + e.get_src().get_stmt() + " -- " + e.get_d() + " --> " + e.get_dest().get_stmt());
			}

			Logger.info("Linking non index phi vars");
			phi_vars.make_phi_links_graph(array_vars, constants);
			array_vars.make_array_var_graph(graph);
			phi_vars.make_non_index_graphs();
			graph.print_def_node_dep_chains(phi_vars, constants);

		}
	}
}
