import java.util.ArrayList;
import java.util.Arrays;
public class Constants {
    static final boolean DEBUG = true;
    private static final String[] EXCLUDES_INTERNAL = new String[] { "jdk.*" };
    static final ArrayList<String> EXCLUDES = new ArrayList<>(Arrays.asList(EXCLUDES_INTERNAL));
    static final String RT_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\rt.jar";
    static final String JCE_PATH_WINDOWS = "C:\\Program Files\\Java\\jdk1.8.0_221\\jre\\lib\\jce.jar";
    static final String RT_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar";
    static final String JCE_PATH_UNIX = "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar";
    static final String COMPILE_CMD = "javac ./resources/src/%s -d ./resources/out";
    static final String RESOURCE_SRC = "./resources/src/";
    static final String RESOURCE_OUT = "./resources/out/";
}


