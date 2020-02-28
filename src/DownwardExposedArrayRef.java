import org.pmw.tinylog.Logger;
import soot.toolkits.graph.Block;

import java.util.HashMap;
import java.util.Map;

class DownwardExposedArrayRef {
    // Map<Block, List<Pair<String, String>>> current_array_version;
    private Block b;
    private Map<String, ArrayVersion> array_ver;

    DownwardExposedArrayRef(Block b) {
        this.b = b;
        this.array_ver = new HashMap<>();
    }

    DownwardExposedArrayRef(DownwardExposedArrayRef daf) {
        this.b = daf.b;
        this.array_ver = new HashMap<>();
        for(Map.Entry<String, ArrayVersion> entry : daf.array_ver.entrySet()) {
            this.array_ver.put(entry.getKey(), new ArrayVersion(entry.getValue()));
        }
    }

    ArrayVersion get(String s) {
        return array_ver.getOrDefault(s, Constants.VAR_NOT_FOUND);
    }

    // CHECKTHIS: this might need to be more explicit (i.e. new_ver(String s, int i) or something)
    void new_ver(String s) {
        if(array_ver.containsKey(s)) {
            ArrayVersion new_ver = array_ver.get(s);
            new_ver.incr_v1();
            array_ver.put(s, new_ver);
        } else {
            Logger.error("Key " + s + " not found, cannot increment version.");
        }
    }

    void put(String s, ArrayVersion av) {
        array_ver.put(s, av);
    }

    @SuppressWarnings("unused")
    boolean contains_var(String s) {
        return array_ver.containsKey(s);
    }

    String get_name(String s) {
        if(array_ver.containsKey(s)) {
            ArrayVersion av = array_ver.get(s);
            String v1 = String.format(Constants.ARR_VER_STR, s, av.get_v1());
            if (av.is_phi()) {
                String v2 = String.format(Constants.ARR_VER_STR, s, av.get_v2());
                return String.format(Constants.ARR_PHI_STR, v1, v2);
            } else {
                return v1;
            }
        } else {
            Logger.error("Key " + s + " not found.");
            return null;
        }
    }
}
