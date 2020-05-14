import soot.ValueBox;

import java.util.Objects;

/**
 * An class representing and index that is used in an array access
 */
class ArrayIndex {
    /**
     * the index value
     */
    private final ValueBox index_valuebox;
    /**
     * true iff we actually have an index (for placeholders)
     */
    private final boolean has_index_flag;

    /**
     * constrictor for the index
     * @param index_valuebox the ValueBox of the index
     */
    ArrayIndex(ValueBox index_valuebox) {
        this.index_valuebox = index_valuebox;
        this.has_index_flag = true;
    }

    /**
     * constructor for an index that has no value (placeholder for
     * created phi nodes and new array statements)
     */
    ArrayIndex() {
        this.index_valuebox = null;
        this.has_index_flag = false;
    }

    /**
     * copy constructor for Index
     * @param i the Index being copied
     */
    ArrayIndex(ArrayIndex i) {
        this.index_valuebox = i.index_valuebox;
        this.has_index_flag = i.has_index_flag;
    }

    /**
     * custom (non overridden) toString() method
     * @return a string representing the Index
     */
    String to_str() {
        if(has_index_flag) {
            assert index_valuebox != null : String.format(Constants.ASSERT_NULL_STR, "index_valuebox");
            return index_valuebox.getValue().toString();
        } else {
            return Constants.ARRAY_VERSION_NEW_ARRAY;
        }
    }

    /**
     * flag to test if the index has been set
     * @return true iff the Index has been set
     */
    @SuppressWarnings("unused")
    boolean index_set() {
        return has_index_flag;
    }

    /**
     * custom (non overridden) equal function
     * @param i the Index that is being tested for equality
     * @return true iff the passed index is equal to this one
     */
    boolean equals(ArrayIndex i) {
        return Objects.equals(i.has_index_flag, has_index_flag) &&
            Objects.equals(to_str(), i.to_str());
    }

}
