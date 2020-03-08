import org.apache.commons.lang3.SystemUtils;
import soot.toolkits.graph.Block;

import java.io.File;
import java.util.List;
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

	static String get_sep() {
		return File.pathSeparator;
//		if (SystemUtils.IS_OS_WINDOWS) {
//			return ";";
//		}
//		return ":";
	}

	static String get_path_sep() {
		return File.separator;
//		if (SystemUtils.IS_OS_WINDOWS) {
//			return ";";
//		}
//		return ":";
	}

	static String get_block_name(Block b) {
		Matcher m = Constants.BLOCK_RE.matcher(b.toString());
		if(m.find()) {
			return m.group(1);
		} else {
			return "Pattern not found.";
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
		for(int i = 0; i < versions.length; i++) {
			sb.append(String.format(Constants.ARR_VER_STR, s, versions[i].get_version()));
			if(i + 1 < versions.length) {
				sb.append(", ");
			} else {
				sb.append(")");
			}
		}
		return String.format("%s_%d = %s;", s, av.get_version(), sb.toString());
	}

	static ArrayVersion rename_av(ArrayVersion av) {
		Index old_index = av.get_index();
		if(av.is_phi()) {
			ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
			return new ArrayVersionPhi(old_index, av_phi.get_array_versions());
		} else {
			ArrayVersionSingle avs = (ArrayVersionSingle)av;
			return new ArrayVersionSingle(1, old_index);
		}
	}

	static boolean all_not_null(List<?> lst) {
		return lst.stream()
				.map(Utils::not_null)
				.reduce(true, Boolean::logicalAnd);
	}

	static <T> boolean not_null(T o) {
		return !Objects.equals(o, null);
	}

}