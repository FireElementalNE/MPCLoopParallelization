import java.util.regex.Pattern;

/**
 * Constants class
 */
class Constants {
    static final String DIFF_ITER_TAG = "DIFFERENT_ITER";
    static final int DEF_LINE_NUM = -2;
    static final int PHI_LINE_NUM = -1;
    static final boolean PRINT_ST = false;
    static final String RT_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\rt.jar";
    static final String JCE_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\jce.jar";
    static final String RT_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar";
    static final String JCE_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";
    static final ArrayVersion VAR_NOT_FOUND = null;
    static final String ARR_VER_STR = "%s_%d%s";
    static final String ARR_PHI_STR_START = "phi(";
    static final Pattern BLOCK_RE = Pattern.compile("^(Block\\s\\d+)");
    static final Pattern BLOCK_NUM_RE = Pattern.compile("^Block\\s(\\d+)");
    static final int BLOCK_RE_FAILED = -1;
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
    static final String Z3_DIR = "z3_python";
    static final String GRAPH_EXT =  ".png";
    static final String EMPTY_STR = "";
    static final String Z3_PORT = "25241";
    static final String Z3_HOST = "localhost";
    static final boolean JUST_COMPILE = false;
    static final String DEFAULT_COMPILE_CMD = "python compile.py -c %s";
    static final String RUN_SOLVER_CMD = "python %s";
    static final String ZERO_TEST_PY_STR_NEG = "%s != 0";
    static final String ZERO_TEST_PY_STR_POS = "%s == 0";
    static final String SAT = "sat";

}





