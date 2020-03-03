import org.apache.commons.lang3.SystemUtils;
import soot.toolkits.graph.Block;

import java.io.File;
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

	static boolean not_null(Object o) {
		return !Objects.equals(o, null);
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
		String v1 = String.format(Constants.ARR_VER_STR, s, copy_av(av_phi.get_av1()).get_version());
		String v2 = String.format(Constants.ARR_VER_STR, s, copy_av(av_phi.get_av2()).get_version());
		String phi_stmt = String.format(Constants.ARR_PHI_STR, v1, v2);
		return String.format("%s_%d = %s;", s, av.get_version(), phi_stmt);
	}

	static ArrayVersion rename_av(ArrayVersion av) {
		Index old_index = av.get_index();
		if(av.is_phi()) {
			ArrayVersionPhi av_phi = (ArrayVersionPhi)av;
			ArrayVersion av1 = copy_av(av_phi.get_av1());
			ArrayVersion av2 = copy_av(av_phi.get_av2());
			return new ArrayVersionPhi(old_index, av1, av2);
		} else {
			ArrayVersionSingle avs = (ArrayVersionSingle)av;
			return new ArrayVersionSingle(1, old_index);
		}
	}
}