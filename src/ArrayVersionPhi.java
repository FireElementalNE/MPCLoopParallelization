public class ArrayVersionPhi implements ArrayVersion {
    private int version;
    private Index index;

    private ArrayVersion av1;
    private ArrayVersion av2;
    private boolean diff_ver_match;

    ArrayVersionPhi(Index index, ArrayVersion av1, ArrayVersion av2) {
        if(av1.get_version() >= av2.get_version()) {
            this.version = av1.get_version() + 1;
        } else {
            this.version = av2.get_version() + 1;
        }
        this.index = index;
        this.av1 = av1;
        this.av2 = av2;
        this.diff_ver_match = av1.get_version() == av2.get_version();
    }

    ArrayVersionPhi(ArrayVersionPhi av_phi) {
        this.version = av_phi.version;
        this.index = av_phi.index;
        this.av1 = av_phi.av1;
        this.av2 = av_phi.av2;
        this.diff_ver_match = av_phi.diff_ver_match;
    }

    ArrayVersion get_av1() {
        return av1;
    }

    ArrayVersion get_av2() {
        return av2;
    }

    @Override
    public boolean is_phi() {
        return true;
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

    @Override
    public boolean has_diff_ver_match() {
        return diff_ver_match;
    }
}
