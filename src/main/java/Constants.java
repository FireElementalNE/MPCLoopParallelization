import java.util.regex.Pattern;

/**
 * Constants class
 */
class Constants {
    static final boolean PRINT_ST = false;
    static final String RT_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\rt.jar";
    static final String JCE_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\jce.jar";
    static final String RT_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar";
    static final String JCE_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";
    static final ArrayVersion VAR_NOT_FOUND = null;
    static final String ARR_VER_STR = "%s_%d";
    static final String ARR_PHI_STR_START = "phi(";
    static final Pattern BLOCK_RE = Pattern.compile("^(Block\\s\\d+)");
    static final Pattern BLOCK_NUM_RE = Pattern.compile("^Block\\s(\\d+)");
    static final String DEFAULT_CP = "test_programs/out";
    static final String DEFAULT_RT_PATH = Utils.rt_path();
    static final String DEFAULT_JCE_PATH = Utils.jce_path();
    static final String ARRAY_VERSION_NEW_ARRAY = "NEW_ARRAY";
    static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    static final char[] ALPHABET_ARRAY = ALPHABET.toCharArray();
    static final String UNDERSCORE = "_";
    static final int GRAPHVIZ_EDGE_WEIGHT = 8;
    static final String GRAPH_DIR = "graphs";
    static final String Z3_DIR = "z3_python";
    static final String GRAPH_EXT =  ".png";
    static final String EMPTY_STR = "";
    static final boolean JUST_COMPILE = false;
    static final String DEFAULT_COMPILE_CMD = "python compile.py -c %s";
    static final String RUN_SOLVER_CMD = "python %s";
    static final String CONSTANTS_PY_STR = "%s == %s";
    // TODO: not sure if this is correct...
    static final String ZERO_TEST_PY_STR_NEG = "%s > 0";
    static final String SAT = "sat";
    static final String ASSERT_NULL_STR = "%s should not be null.";
    // BLANK NODE PLACEHOLDERS
    static final String EMPTY_DEF_USE = "EMPTY_DEF_USE";
    static final String EMPTY_FLOW_GRAPH = "EMPTY_FLOW_GRAPH";
    static final String EMPTY_VAR_GRAPH = "EMPTY_VAR_GRAPH: %s";
    static final String EMPTY_NON_INDEX_GRAPH = "EMPTY_NON_INDEX_GRAPH: %s";
    static final String EMPTY_PHI_LINKS = "EMPTY_PHI_LINKS: %s";
    static final String EMPTY_SCC = "EMPTY_SCC";
    static final String EMPTY_VAR = "EMPTY_VAR: %s";
    static final String EMPTY_VAR_DEP = "EMPTY_VAR_DEP: %s && %s";
}





