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

}