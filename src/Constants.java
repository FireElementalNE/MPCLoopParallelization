import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
class Constants {
    private static final String[] EXCLUDES_INTERNAL = new String[] { "jdk.*" };
    static final ArrayList<String> EXCLUDES = new ArrayList<>(Arrays.asList(EXCLUDES_INTERNAL));
    static final String RT_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\rt.jar";
    static final String JCE_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\jce.jar";
    static final String RT_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar";
    static final String JCE_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";
    static final String UTILS_JAVA_FILE = "src/Utils.java";
    static final String COMPILE_CMD = "javac %s -d %s -cp %s";
    static final String RESOURCE_SRC = "./test_programs/src/";
    static final String RESOURCE_OUT = "./test_programs/out/";
    static final String SSA_ARRAY_CPY = "arraycopy(%s, %s)";
    static final String SSA_ASSIGNMENT = "%s[%s] =  %s";
    static final String SSA_PHI = "phi(%s, %s)";
    static final String SSA_NAME = "%s_%d";
    static final String ARRAY_REF = "%s[%s]";
    static final String DEPENDS_STMT = "Use '%s' depends on Definition '%s'";
    static final ArrayVersion VAR_NOT_FOUND = null;
    static final int NO_VER_SET = -1;
    static final int INIT_ARR_VER = 1;
    static final String ARR_VER_STR = "%s_%d";
    static final String ARR_PHI_STR = "phi(%s, %s)";
    static final Pattern BLOCK_RE = Pattern.compile("^(Block\\s\\d+)");
    static final String DEFAULT_CP = "test_programs/out";
    static final String DEFAULT_SD = "test_programs/src";
    static final String DEFAILT_RT_PATH = Utils.rt_path();
    static final String DEFAILT_JCE_PATH = Utils.jce_path();
    static final String ARRAY_VERSION_NEW_ARRAY = "NEW_ARRAY";
    static final String INT_TYPE = "int";
}



