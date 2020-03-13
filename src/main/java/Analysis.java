import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.tinylog.Logger;
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

import java.io.File;
import java.io.IOException;
import java.util.*;

import static guru.nidi.graphviz.model.Factory.*;


public class Analysis extends BodyTransformer {
	private Map<Block, DownwardExposedArrayRef> c_arr_ver; // current array version
	private LinkedList<Block> worklist;
	private Set<Block> seen_blocks;
	private Set<ImmutablePair<String, String>> loop_head_exits;
	private Map<String, ArrayVersion> array_vars;
	private List<Variable> vars;
	private Set<PhiVariable> phi_vars;
	private ArrayDefUseGraph graph;
	private guru.nidi.graphviz.model.MutableGraph final_graph;
	private guru.nidi.graphviz.model.MutableGraph flow_graph;
	// TODO: need to finish indexes!
	// TODO: need to finish loop dependent changes!
	public Analysis(String class_name) {
		final_graph = mutGraph(class_name + "_final").setDirected(true);
		flow_graph = mutGraph(class_name + "_flow").setDirected(true);
		seen_blocks = new HashSet<>();
		c_arr_ver = new HashMap<>();
		worklist = new LinkedList<>();
		phi_vars = new HashSet<>();
		loop_head_exits = new HashSet<>();
		array_vars = new HashMap<>();
		graph = new ArrayDefUseGraph();
		vars = new ArrayList<>();
	}

	private void make_graph_png() {
		for(Map.Entry<Integer, Edge> entry: graph.get_edges().entrySet()) {
			Edge e = entry.getValue();
			Node def = e.get_def();
			Node use = e.get_use();
			guru.nidi.graphviz.model.Node def_node = node(def.get_stmt());
			guru.nidi.graphviz.model.Node use_node = node(use.get_stmt());
			final_graph.add(def_node.link(to(use_node).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
		}
		try {
			Graphviz.fromGraph(final_graph).width(Constants.GRAPHVIZ_WIDTH).render(Format.PNG)
					.toFile(new File(Utils.make_graph_name(final_graph.name().toString())));
		} catch (IOException e) {
			e.printStackTrace();
		}

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

	private void add_flow_edge(Block from_blk, Block to_blk, boolean is_loop) {
		guru.nidi.graphviz.model.Node from_node = node(Utils.get_block_name(from_blk));
		guru.nidi.graphviz.model.Node to_node = node(Utils.get_block_name(to_blk));
		if(is_loop) {
			flow_graph.add(from_node.link(to(to_node).with(Style.DASHED, LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
		} else {
			flow_graph.add(from_node.link(to(to_node).with(LinkAttr.weight(Constants.GRAPHVIZ_EDGE_WEIGHT))));
		}
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

		// Logger.info(Utils.fill_spaces(sc) + "Found Block (head is first line).");
		// Logger.info(Utils.fill_spaces(sc) + "Number of successor Blocks: " + b.getSuccs().size());
		// Logger.info(Utils.fill_spaces(sc) + "Preds size: " + b.getPreds().size());
		Logger.debug(Utils.get_block_name(b) + " head: " + b.getHead().toString());
		for(Iterator<Unit> i = b.iterator(); i.hasNext();) {
			ArrayVariableVisitor visitor = new ArrayVariableVisitor(array_vars, graph);
			VariableVisitor var_visitor = new VariableVisitor(vars, phi_vars);
			Unit u = i.next();
			u.apply(visitor);
			u.apply(var_visitor);
			this.array_vars = visitor.get_vars();
			this.graph = visitor.get_graph();
			this.vars = var_visitor.get_vars();
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
				add_flow_edge(b, sb, false);
				seen_blocks.add(b);
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

	private boolean check_c_arr_ver(List<Block> preds_list) {
		Set<Block> possible = c_arr_ver.keySet();
		Set<Block> preds_set = new HashSet<>(preds_list);
		return possible.containsAll(preds_set);
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void process(Block b, Block head, List<String> exits) {
		if(seen_blocks.contains(b)) {
			Logger.error("Were have seen this block: " + b.getHead().toString());
			return;
		}
		// TODO: update indexes!
		Logger.info(Utils.get_block_name(b) + ": " + b.getHead().toString());
		List<Block> pred_blocks = b.getPreds();
		Block b1 = pred_blocks.get(0);
		if(pred_blocks.size() > 1) { // we have a merge
			if (check_c_arr_ver(pred_blocks)) {
				for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
					if (Utils.all_not_null(pred_blocks)) {
						List<ArrayVersion> avs = new ArrayList<>();
						for(Block blk : pred_blocks) {
							DownwardExposedArrayRef daf = new DownwardExposedArrayRef(c_arr_ver.get(blk));
							avs.add(Utils.copy_av(daf.get(entry.getKey())));
						}
						// make a phi node!
						// Indexes MUST be the same!
						ArrayVersionPhi av_phi = new ArrayVersionPhi(new Index(null), avs);
						DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
						new_daf.put(entry.getKey(), av_phi);
						Logger.info("We made a phi node: " + new_daf.get_name(entry.getKey()));
						Node n = new Node(entry.getKey(), av_phi);
						graph.add_node(n, true);
						c_arr_ver.put(b, new_daf);
					} else {
						Logger.info("False phi found!");
						DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
						c_arr_ver.put(b, new_daf);
					}
				}
			} else {
				Logger.info("Both preds should already be in c_arr_ver!");
				Logger.info("Skipping and adding this block to the back of worklist.");
				worklist.addLast(b);
				return;
			}
		} else { // we do not have a merge
			if(!exits.contains(pred_blocks.get(0).getHead().toString())) {
				DownwardExposedArrayRef new_daf = new DownwardExposedArrayRef(b);
				if (c_arr_ver.containsKey(b1)) {
					for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
						// CHECKTHIS: throwing NullPointerException because of aliasing issue (see top of class)
						ArrayVersion current_s = Utils.copy_av(c_arr_ver.get(b1).get(entry.getKey()));
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
			VariableVisitor var_visitor = new VariableVisitor(vars, phi_vars);
			Unit u = i.next();
			u.apply(bfs_visitor);
			u.apply(av_visitor);
			u.apply(var_visitor);
			array_vars = av_visitor.get_vars();
			c_arr_ver = bfs_visitor.get_c_arr_ver();
			graph = bfs_visitor.get_graph();
			vars = var_visitor.get_vars();
			phi_vars = var_visitor.get_phi_vars();
		}
		List<Block> succ_blocks = b.getSuccs();
		Logger.debug("We found " + succ_blocks.size() + " successor blocks.");
		if(loop_head_exits.contains(new ImmutablePair<>(head.toString(), b.toString()))) {
			Logger.info("We found an exit, stopping.");
		} else {
			for(Block s1 : succ_blocks) {
				if (!seen_blocks.contains(s1)) {
					add_flow_edge(b, s1, false);
					worklist.addLast(s1);
				}
				else if(Utils.get_block_name(s1).equals(Utils.get_block_name(head))) {
					Logger.info("We found the head! We are entering a new second iteration!");
					add_flow_edge(b, head, true);
				}
			}
		}
		seen_blocks.add(b);
	}

	private void init_BFS_vars(Block b) {
		DownwardExposedArrayRef down_ar = new DownwardExposedArrayRef(b);
		for (Map.Entry<String, ArrayVersion> entry : array_vars.entrySet()) {
			down_ar.put(entry.getKey(), Utils.copy_av(entry.getValue()));
		}
		c_arr_ver.put(b, down_ar);
	}

	private void BFS(Block head, List<String> exits) {
		seen_blocks.add(head);
		init_BFS_vars(head);
		// CHECKTHIS: Assuming we only have one head...
		for(Block b : head.getSuccs()) {
			String head_str = head.getHead().toString();
			String exit_str = b.getHead().toString();
			ImmutablePair<String, String> head_exit = new ImmutablePair<>(head_str, exit_str);
			if(loop_head_exits.contains(head_exit)) {
				Logger.debug("Found an exit, skipping.");
			} else {
				add_flow_edge(head, b, false);
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
		for(Map.Entry<String, String> e : options.entrySet()) {
			Logger.debug(e.getKey() + " ----> " + e.getValue());
		}
		find_loop_heads(body);
		parse_blocks_start(body);

		Logger.info("Node count: " + graph.get_nodes().size());
		for(Map.Entry<String, Node> entry : graph.get_nodes().entrySet()) {
			Logger.info(entry.getKey() + " -> " + entry.getValue().get_stmt());
		}
		Logger.info("Edge count: " + graph.get_edges().size());
		for(Map.Entry<Integer, Edge> entry : graph.get_edges().entrySet()) {
			Logger.info(entry.getKey() + ": ");
			Logger.info(" " + entry.getValue().get_def().get_stmt());
			Logger.info(" " + entry.getValue().get_use().get_stmt());
		}
		make_graph_png();
		try {
			Graphviz.fromGraph(flow_graph).width(Constants.GRAPHVIZ_WIDTH).render(Format.PNG)
					.toFile(new File(Utils.make_graph_name(flow_graph.name().toString())));
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(Variable v : vars) {
			v.make_graph();
		}
	}
}
