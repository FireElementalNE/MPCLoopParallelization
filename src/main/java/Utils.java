import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import org.apache.commons.lang3.SystemUtils;
import org.tinylog.Logger;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.shimple.PhiExpr;
import soot.toolkits.graph.Block;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Generic Utility class
 */
@SuppressWarnings("unused")
class Utils {

	/**
	 * delete a directory
	 * @param dir a directory as a File object
	 * @return true iff the directory was deleted successfully
	 */
	// Found here:
	// https://javarevisited.blogspot.com/2015/03/how-to-delete-directory-in-java-with-files.html
	// Thanks!
	static boolean deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			File[] children = dir.listFiles();
			assert children != null : String.format(Constants.ASSERT_NULL_STR, "children");
			for (File child : children) {
				boolean success = deleteDirectory(child);
				if (!success) {
					return false;
				}
			}
		}
		Logger.debug("removing file or directory : " + dir.getName());
		return dir.delete();
	}

	/**
	 * refresh (delete if exists) directory
	 * @param dir_name the name of the directory
	 */
	static void refresh_dir(String dir_name) {
		File dir = new File(dir_name);
		boolean rc;
		if(dir.exists()) {
			rc = Utils.deleteDirectory(dir);
			if (!rc) {
				Logger.error(dir.getName() + " could not be deleted, exiting.");
				System.exit(0);
			}
		}
		rc = dir.mkdir();
		if(!rc) {
			Logger.error(dir.getName() + " could not be created, exiting.");
			System.exit(0);
		}
	}

	/**
	 * execute a command on the cl and get the stdout
	 * @param cmd the command
	 * @return the stdout of the command
	 */
	static List<String> execute_cmd_ret(String cmd) {
		List<String> stdouts = new ArrayList<>();
		boolean errors_found = false;
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			BufferedReader stdError = new BufferedReader(new
					InputStreamReader(p.getErrorStream()));
			BufferedReader stdInput = new BufferedReader(new
					InputStreamReader(p.getInputStream()));
			String s;
			while ((s = stdInput.readLine()) != null) {
				stdouts.add(s);
			}
			while((s = stdError.readLine()) != null) {
				Logger.error(s);
				errors_found = true;
			}
		} catch (IOException | InterruptedException e) {
			Logger.error("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if(Constants.PRINT_ST) {
				e.printStackTrace();
			}
		}
		if(errors_found) {
			Logger.error("Errors found. Exiting.");
			System.exit(0);
		}
		return stdouts;
	}

	/**
	 * execute a command on the cl
	 * @param cmd the command
	 */
	static void execute_cmd(String cmd) {
		boolean errors_found = false;
		try {
			Process p = Runtime.getRuntime().exec(cmd);

			BufferedReader stdError = new BufferedReader(new
					InputStreamReader(p.getErrorStream()));
			BufferedReader stdInput = new BufferedReader(new
					InputStreamReader(p.getInputStream()));
			String s;
			while ((s = stdInput.readLine()) != null) {
				Logger.info(s);
			}
			while((s = stdError.readLine()) != null) {
				Logger.error(s);
				errors_found = true;
			}
		} catch (IOException e) {
			Logger.error("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if(Constants.PRINT_ST) {
				e.printStackTrace();
			}
		}
		if(errors_found) {
			Logger.error("Errors found. Exiting.");
			System.exit(0);
		}
	}


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
			return Integer.parseInt(m.group(1));
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
		assert av instanceof ArrayVersionPhi : "can only create phi statements for array versions are in are phi nodes.";
		ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
		StringBuilder sb = new StringBuilder();
		sb.append(Constants.ARR_PHI_STR_START);
		ArrayVersion[] versions = av_phi.get_array_versions().toArray(new ArrayVersion[0]);
		Map<Integer, Integer> have_seen = new HashMap<>();
		for(int i = 0; i < versions.length; i++) {
			// String aug = Constants.EMPTY_STR;
//			if(av_phi.has_diff_ver_match()) {
//				if(have_seen.containsKey(versions[i].get_version())) {
//					have_seen.put(versions[i].get_version(), have_seen.get(versions[i].get_version()) + 1);
//				} else {
//					have_seen.put(versions[i].get_version(), 0);
//				}
//				aug = String.valueOf(Constants.ALPHABET_ARRAY[have_seen.get(versions[i].get_version())]);
//			}
			sb.append(String.format(Constants.ARR_VER_STR, basename, versions[i].get_version()));
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
	 * @param default_case the default string that to be shown on an empty map
	 */
	static void print_graph(MutableGraph graph, String default_case) {
		String graph_name = graph.name().toString();
		Logger.debug(graph_name + ":");
		Logger.debug("\tNodes: " + graph.nodes().size());
		Logger.debug("\tEdges: " + graph.edges().size());
		// TODO: fix this
		/*if(graph.nodes().size() < 1) {
			guru.nidi.graphviz.model.Node default_node =
					node(default_case).with(guru.nidi.graphviz.attribute.Shape.RECTANGLE, Style.FILLED, Color.GRAY);
			graph.add(default_node);
		}*/
		try {
			BufferedImage bimg = Graphviz.fromGraph(graph).render(Format.PNG).toImage();
			File f = new File(Utils.make_graph_name(graph.name().toString()));
			ImageIO.write(bimg, "png", f);
			Logger.info(graph_name + " has " + graph.nodes().size() + " nodes.");
			Logger.info(graph_name + " has " + graph.edges().size() + " edges.");
		} catch (IOException | AWTError | NoClassDefFoundError e) {
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

	/**
	 * get the uses of a PhiExpr (not including the entire statement)
	 * @param pexpr the PhiExpr
	 * @return a list of PhiExr uses (as strings)
	 */
	static List<String> get_phi_var_uses_as_str(PhiExpr pexpr) {
		// TODO: this is wacky, it always adds the full expression as the first thing. might need to fix this
		return pexpr.getValues().stream().map(Object::toString).collect(Collectors.toList());
	}

	/**
	 * get the uses of an Assignment statement  (not including the entire statement)
	 * @param stmt the AssignStmt
	 * @return a list of AssignStmt uses (as strings)
	 */
	static List<String> get_assignment_uses_as_str(AssignStmt stmt) {
		// TODO: this is wacky, it always adds the full expression as the first thing. might need to fix this
		return stmt.getUseBoxes().stream().map(el -> el.getValue().toString())
				.filter(el -> !Objects.equals(el, stmt.getRightOp().toString())).collect(Collectors.toList());
	}

	/**
	 * test if a Mutable Graph contains a node
	 * @param graph the graph
	 * @param node the node name
	 * @return true iff there is a node in the graph with the same name as the passed node
	 */
	static boolean contains_node(MutableGraph graph, String node) {
		return graph.nodes().stream().map(MutableNode::toString).collect(Collectors.toList()).contains(node);
	}

}
