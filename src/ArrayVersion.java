@SuppressWarnings("unused")

class ArrayVersion {
    private int v1;
    private int v2;
    private boolean is_phi_node;

    ArrayVersion(int v1) {
        this.v1 = v1;
        this.v2 = Constants.NO_VER_SET;
        this.is_phi_node = false;
    }

    ArrayVersion(int v1, int v2) {
        this.v1 = v1;
        this.v2 = v2;
        this.is_phi_node = true;
    }

    ArrayVersion(ArrayVersion av) {
        // CHECKTHIS: throwing NullPointerException because of aliasing issue (see top Analysis.java)
        this.v1 = av.v1;
        this.v2 = av.v2;
        this.is_phi_node = av.is_phi_node;
    }

    boolean is_phi() {
        return is_phi_node;
    }

    void set_v1(int new_v1) {
        assert !is_phi_node;
        v1 = new_v1;
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
