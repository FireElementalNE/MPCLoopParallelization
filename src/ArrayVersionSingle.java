import org.pmw.tinylog.Logger;

class ArrayVersionSingle implements ArrayVersion {
    private int version;
    private boolean diff_ver_match;
    private Index index;

    ArrayVersionSingle(int version, Index index) {
        this.version = version;
        this.diff_ver_match = false;
        this.index = index;
        this.diff_ver_match = false;
    }

    ArrayVersionSingle(ArrayVersionSingle av) {
        this.version = av.version;
        this.diff_ver_match = av.diff_ver_match;
        this.index = av.index;
        this.diff_ver_match = false;
    }

    @Override
    public boolean is_phi() {
        return false;
    }

    @Override
    public boolean has_diff_ver_match() {
        return diff_ver_match;
    }

    @Override
    public void incr_version() {
        version++;
    }

    @Override
    public int get_version() {
        return version;
    }

    @Override
    public Index get_index() {
        return index;
    }
}
