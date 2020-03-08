import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayVersionPhi implements ArrayVersion {
    private int version;
    private Index index;

    private List<ArrayVersion> array_versions;

    private boolean diff_ver_match;

    ArrayVersionPhi(Index index, List<ArrayVersion> array_versions) {
        this.version = array_versions.stream()
                .map(ArrayVersion::get_version)
                .reduce(Integer.MIN_VALUE, Math::max);
        this.index = index;
        this.array_versions = array_versions;
        List<Integer> versions = array_versions.stream().map(ArrayVersion::get_version).collect(Collectors.toList());
        this.diff_ver_match = versions.size() != new HashSet<>(versions).size();
    }

    ArrayVersionPhi(ArrayVersionPhi av_phi) {
        this.version = av_phi.version;
        this.index = av_phi.index;
        this.array_versions = av_phi.array_versions;
        this.diff_ver_match = av_phi.diff_ver_match;
    }

    List<ArrayVersion> get_array_versions() {
        List <ArrayVersion> tmp = new ArrayList<>();
        for(ArrayVersion av : array_versions) {
            tmp.add(Utils.copy_av(av));
        }
        return tmp;
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
