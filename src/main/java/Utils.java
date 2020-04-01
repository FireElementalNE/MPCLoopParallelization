import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import org.apache.commons.lang3.SystemUtils;
import org.tinylog.Logger;
import soot.toolkits.graph.Block;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

class Utils {

	static String rt_path() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return Constants.RT_PATH_WINDOWS;
		}
		return Constants.RT_PATH_UNIX;
	}

	static String jce_path() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return Constants.JCE_PATH_WINDOWS;
		}
		return Constants.JCE_PATH_UNIX;
	}

	static String get_block_name(Block b) {
		Matcher m = Constants.BLOCK_RE.matcher(b.toString());
		if(m.find()) {
			return m.group(1);
		} else {
			Logger.error("Pattern not found.");
			return null;
		}
	}

	static int get_block_num(Block b) {
		Matcher m = Constants.BLOCK_NUM_RE.matcher(b.toString());
		if(m.find()) {
			return new Integer(m.group(1));
		} else {
			Logger.error("Pattern not found.");
			return -1;
		}
	}

	static ArrayVersion copy_av(ArrayVersion av) {
		if(av.is_phi()) {
			return new ArrayVersionPhi((ArrayVersionPhi)av);
		} else {
			return new ArrayVersionSingle((ArrayVersionSingle)av);
		}
	}

	static String create_phi_stmt(String s, ArrayVersion av) {
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
			sb.append(String.format(Constants.ARR_VER_STR, s, versions[i].get_version(), aug));
			if(i + 1 < versions.length) {
				sb.append(", ");
			} else {
				sb.append(")");
			}
		}
		return String.format("%s_%d = %s;", s, av.get_version(), sb.toString());
	}



	static ArrayVersion rename_av(ArrayVersion av) {
		if(av.is_phi()) {
			ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
			return new ArrayVersionPhi(av_phi);
		} else {
			ArrayVersionSingle avs = (ArrayVersionSingle)av;
			return new ArrayVersionSingle(avs);
		}
	}

	static String make_graph_name(String basename) {
		return String.format("%s%s%s%s", Constants.GRAPH_DIR, File.separator, basename, Constants.GRAPH_EXT);
	}

	static boolean all_not_null(List<?> lst) {
		return lst.stream()
				.map(Utils::not_null)
				.reduce(true, Boolean::logicalAnd);
	}

	static <T> boolean not_null(T o) {
		return !Objects.equals(o, null);
	}

	static void print_graph(MutableGraph graph) {
		try {
			Graphviz.fromGraph(graph).width(Constants.GRAPHVIZ_WIDTH).render(Format.PNG)
					.toFile(new File(Utils.make_graph_name(graph.name().toString())));
		} catch (IOException | java.awt.AWTError | java.lang.NoClassDefFoundError e) {
			Logger.error("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if(Constants.PRINT_ST) {
				e.printStackTrace();
			}
		}
	}

}
