import org.apache.commons.lang3.SystemUtils;
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
		if (SystemUtils.IS_OS_WINDOWS) {
			return ";";
		}
		return ":";
	}
}