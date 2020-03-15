import java.util.regex.Pattern;


class Constants {
//    private static final String[] EXCLUDES_INTERNAL = new String[] { "jdk.*" };
//    static final ArrayList<String> EXCLUDES = new ArrayList<>(Arrays.asList(EXCLUDES_INTERNAL));
    static final String RT_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\rt.jar";
    static final String JCE_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\jce.jar";
    static final String RT_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar";
    static final String JCE_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";
//    static final String UTILS_JAVA_FILE = "src/Utils.java";
//    static final String SSA_ARRAY_CPY = "arraycopy(%s, %s)";
//    static final String SSA_ASSIGNMENT = "%s[%s] =  %s";
//    static final String SSA_PHI = "phi(%s, %s)";
//    static final String SSA_NAME = "%s_%d";
//    static final String ARRAY_REF = "%s[%s]";
//    static final String DEPENDS_STMT = "Use '%s' depends on Definition '%s'";
    static final ArrayVersion VAR_NOT_FOUND = null;
//    static final int NO_VER_SET = -1;
//    static final int INIT_ARR_VER = 1;
    static final String ARR_VER_STR = "%s_%d";
    static final String ARR_PHI_STR_START = "phi(";
    static final Pattern BLOCK_RE = Pattern.compile("^(Block\\s\\d+)");
    static final String DEFAULT_CP = "test_programs/out";
    static final String DEFAILT_RT_PATH = Utils.rt_path();
    static final String DEFAILT_JCE_PATH = Utils.jce_path();
    static final String ARRAY_VERSION_NEW_ARRAY = "NEW_ARRAY";
    static final String INT_TYPE = "int";
    static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    static final char[] ALPHABET_ARRAY = ALPHABET.toCharArray();
    static final String UNDERSCORE = "_";
    static final int GRAPHVIZ_EDGE_WEIGHT = 8;
    static final int GRAPHVIZ_WIDTH = 700;
    static final String GRAPH_DIR = "graphs";
    static final String GRAPH_EXT =  ".png";
}



