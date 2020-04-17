import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subclass of the ArrayVersion interface
 * Defines logic for phi array variables.
 */
@SuppressWarnings("FieldMayBeFinal")
public class ArrayVersionPhi implements ArrayVersion {


    private int version;
    private List<ArrayVersion> array_versions;
    private boolean diff_ver_match;
    private int block;
    private boolean has_been_written_to;

    /**
     * create a new ArrayVersionPhi
     * @param array_versions the individual array versions that compose this phi variable
     * @param block the block that created this AV
     */
    ArrayVersionPhi(List<ArrayVersion> array_versions, int block) {
        this.version = array_versions.stream()
                .map(ArrayVersion::get_version)
                .reduce(Integer.MIN_VALUE, Math::max) + 1;
        this.array_versions = array_versions;
        List<Integer> versions = array_versions.stream().map(ArrayVersion::get_version).collect(Collectors.toList());
        this.diff_ver_match = versions.size() != new HashSet<>(versions).size();
        this.block = block;
        this.has_been_written_to = false;
    }

    /**
     * Copy constructor for an ArrayVersionPhi
     * @param av_phi the original ArrayVersionPhi
     */
    ArrayVersionPhi(ArrayVersionPhi av_phi) {
        this.version = av_phi.version;
        this.array_versions = av_phi.array_versions;
        this.diff_ver_match = av_phi.diff_ver_match;
        this.block = av_phi.block;
        this.has_been_written_to = av_phi.has_been_written_to;
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
     * @param block the block that changed this AV
     */
    @Override
    public void incr_version(int block) {
        this.block = block;
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

    /**
     * get the last block to change this AV
     * @return the last block to change this AV
     */
    @Override
    public int get_block() {
        return block;
    }

    /**
     * getter returning whether array has been written to
     * @return true iff the array var has been written to
     */
    @Override
    public boolean has_been_written_to() {
        return false;
    }

    /**
     * setter to set the has been written to flag
     */
    @Override
    public void toggle_written() {
        this.has_been_written_to = true;
    }

}
