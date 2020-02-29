import org.pmw.tinylog.Logger;

class ArrayVersion {
    private int v1;
    private int v2;
    private boolean is_phi_node;
    private boolean diff_ver_match;

    ArrayVersion(int v1) {
        this.v1 = v1;
        this.v2 = Constants.NO_VER_SET;
        this.is_phi_node = false;
        this.diff_ver_match = false;
    }

    ArrayVersion(int v1, int v2) {
        this.v1 = v1;
        this.v2 = v2;
        if(v1 == v2) {
            Logger.info("We have matching ids for different versions. ");
            this.diff_ver_match = true;
        }
        this.is_phi_node = true;
    }

    ArrayVersion(ArrayVersion av) {
        this.v1 = av.v1;
        this.v2 = av.v2;
        this.is_phi_node = av.is_phi_node;
        this.diff_ver_match = av.diff_ver_match;
    }

    boolean is_phi() {
        return is_phi_node;
    }

    boolean has_diff_ver_match() {
        return diff_ver_match;
    }

    void incr_v1() {
        assert !is_phi_node;
        v1++;
    }

    int get_v1() {
        return v1;
    }

    int get_v2() {
        assert is_phi_node;
        return v2;
    }

}
