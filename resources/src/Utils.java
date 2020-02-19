import java.lang.reflect.Array;
import java.util.Arrays;
class Utils {
    static  Object arraycopy(Object a) {
        if(a.getClass().isArray()) {
            Object b = a; // Not a true deep copy...
            return b;
        }
        return null;
    }
    static void phi(Object a, Object a1) {}
}