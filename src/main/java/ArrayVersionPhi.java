import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subclass of the ArrayVersion interface
 * Defines logic for phi array variables.
 */
public class ArrayVersionPhi implements ArrayVersion {


    private int version;
    private List<ArrayVersion> array_versions;
    private boolean diff_ver_match;

    /**
     * create a new ArrayVersionPhi
     * @param array_versions the individual array versions that compose this phi variable
     */
    ArrayVersionPhi(List<ArrayVersion> array_versions) {
        this.version = array_versions.stream()
                .map(ArrayVersion::get_version)
                .reduce(Integer.MIN_VALUE, Math::max) + 1;
        this.array_versions = array_versions;
        List<Integer> versions = array_versions.stream().map(ArrayVersion::get_version).collect(Collectors.toList());
        this.diff_ver_match = versions.size() != new HashSet<>(versions).size();
    }

    /**
     * Copy constructor for an ArrayVersionPhi
     * @param av_phi the original ArrayVersionPhi
     */
    ArrayVersionPhi(ArrayVersionPhi av_phi) {
        this.version = av_phi.version;
        this.array_versions = av_phi.array_versions;
        this.diff_ver_match = av_phi.diff_ver_match;
    }

    /**
     * getter for the List of Array versions composing this ArrayVersionPhi
     * @return the List of ArrayVersion in this ArrayVersionPhi
     */
    List<ArrayVersion> get_array_versions() {
        List <ArrayVersion> tmp = new ArrayList<>();
        for(ArrayVersion av : array_versions) {
            tmp.add(Utils.copy_av(av));
        }
        return tmp;
    }

    /**
     * Overridden getter
     * @return true always, this always represents a phi node
     */
    @Override
    public boolean is_phi() {
        return true;
    }

    /**
     * Overridden version incrementer, increases the version by 1
     */
    @Override
    public void incr_version() {
        version++;
    }

    /**
     * Overridden getter return version
     * @return the current version
     */
    @Override
    public int get_version() {
        return version;
    }

    /**
     * Overridden getter, returns diff_ver_match
     * @return true iff the variables in the phi node have the same name (this can happen on branching)
     */
    @Override
    public boolean has_diff_ver_match() {
        return diff_ver_match;
    }

}
