import guru.nidi.graphviz.attribute.ForGraph;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Rasterizer;
import guru.nidi.graphviz.model.MutableAttributed;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.SystemUtils;
import org.tinylog.Logger;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * Generic Utility class
 */
class Utils {
	/**
	 * get the RT path depending on OS
	 * @return get the default path to rt.jar
	 */
	static String rt_path() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return Constants.RT_PATH_WINDOWS;
		}
		return Constants.RT_PATH_UNIX;
	}


	/**
	 * get the JCE path depending on OS
	 * @return get the default path to jce.jar
	 */
	static String jce_path() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return Constants.JCE_PATH_WINDOWS;
		}
		return Constants.JCE_PATH_UNIX;
	}

	/**
	 * For convenience... get the name of a block!
	 * @param b the Block
	 * @return the name of the block (in the form 'Block 1')
	 */
	static String get_block_name(Block b) {
		Matcher m = Constants.BLOCK_RE.matcher(b.toString());
		if(m.find()) {
			return m.group(1);
		} else {
			Logger.error("Pattern not found.");
			return null;
		}
	}

	/**
	 * For convenience... get the number of a block!
	 * @param b the Block
	 * @return the number of the block
	 */
	static int get_block_num(Block b) {
		Matcher m = Constants.BLOCK_NUM_RE.matcher(b.toString());
		if(m.find()) {
			return new Integer(m.group(1));
		} else {
			Logger.error("Pattern not found.");
			return -1;
		}
	}

	/**
	 * copy an ArrayVersion
	 * @param av the ArrayVersion
	 * @return a (hopefully) deep copy the passed ArrayVersion
	 */
	static ArrayVersion copy_av(ArrayVersion av) {
		if(av.is_phi()) {
			return new ArrayVersionPhi((ArrayVersionPhi)av);
		} else {
			return new ArrayVersionSingle((ArrayVersionSingle)av);
		}
	}

	/**
	 * Create String representation of a Phi statement for arrays
	 * If the array versions are the same adds letters to differentiate
	 * @param basename the basename of the phi statement
	 * @param av the array version
	 * @return a String representation of the array version
	 */
	static String create_phi_stmt(String basename, ArrayVersion av) {
		assert av instanceof ArrayVersionPhi;
		ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
		StringBuilder sb = new StringBuilder();
		sb.append(Constants.ARR_PHI_STR_START);
		ArrayVersion[] versions = av_phi.get_array_versions().toArray(new ArrayVersion[0]);
		Map<Integer, Integer> have_seen = new HashMap<>();
		for(int i = 0; i < versions.length; i++) {
			String aug = Constants.EMPTY_STR;
			if(av_phi.has_diff_ver_match()) {
				if(have_seen.containsKey(versions[i].get_version())) {
					have_seen.put(versions[i].get_version(), have_seen.get(versions[i].get_version()) + 1);
				} else {
					have_seen.put(versions[i].get_version(), 0);
				}
				aug = String.valueOf(Constants.ALPHABET_ARRAY[have_seen.get(versions[i].get_version())]);
			}
			sb.append(String.format(Constants.ARR_VER_STR, basename, versions[i].get_version(), aug));
			if(i + 1 < versions.length) {
				sb.append(", ");
			} else {
				sb.append(")");
			}
		}
		return String.format("%s_%d = %s;", basename, av.get_version(), sb.toString());
	}


	/**
	 * rename an ArrayVersion
	 * @param av the ArrayVersion
	 * @return the ArrayVersion renamed
	 */
	static ArrayVersion rename_av(ArrayVersion av) {
		if(av.is_phi()) {
			ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
			return new ArrayVersionPhi(av_phi);
		} else {
			ArrayVersionSingle avs = (ArrayVersionSingle)av;
			return new ArrayVersionSingle(avs);
		}
	}

	/**
	 * create the name of a dot graph
	 * @param basename the basename for the graph
	 * @return the name of the graph with the required constants added
	 */
	static String make_graph_name(String basename) {
		return String.format("%s%s%s%s", Constants.GRAPH_DIR, File.separator, basename, Constants.GRAPH_EXT);
	}

	/**
	 * AMAZING universal checker to determine if every element of a list is not null
	 * @param lst the list
	 * @return true iff no member of the list is null
	 */
	static boolean all_not_null(List<?> lst) {
		return lst.stream()
				.map(Utils::not_null)
				.reduce(true, Boolean::logicalAnd);
	}

	/**
	 * AMAZING universal checker to determine if something is null
	 * @param o the something
	 * @param <T> the type of the something
	 * @return true iff the something is null
	 */
	static <T> boolean not_null(T o) {
		return !Objects.equals(o, null);
	}

	/**
	 * Write a Mutable GraphViz to disk
	 * @param graph the graph
	 */
	static void print_graph(MutableGraph graph) {
		MutableAttributed<MutableGraph, ForGraph> z = graph.graphAttrs();
		try {
			Graphviz viz = Graphviz.fromGraph(graph);
//			viz.render(Format.PNG);
			viz.rasterize(Rasterizer.BATIK).toFile(new File(Utils.make_graph_name(graph.name().toString())));
//			.render(Format.PNG)
//					.toFile(new File(Utils.make_graph_name(graph.name().toString())));
		} catch (IOException | java.awt.AWTError | java.lang.NoClassDefFoundError e) {
			Logger.error("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if(Constants.PRINT_ST) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * check to see if an assignment statement contains an array ref, if it does then checks if the array ref is
	 * on the left hand side (is a def)
	 * @param stmt the statement
	 * @return true iff the assignment stmt contains an array reference and the reference is on the left hand side
	 */
	static boolean is_def(Stmt stmt) {
		if(!(stmt instanceof AssignStmt)) {
			// TODO: not sure how to handle other atm. In index visitor we pass all types of statements
			//       but it only matters if it is an AssignStmt
			return false;
		}
		AssignStmt astmt = (AssignStmt)stmt;
		if(!stmt.containsArrayRef()) {
			return false;
		}
		return astmt.getLeftOp() instanceof ArrayRef;
	}

}
