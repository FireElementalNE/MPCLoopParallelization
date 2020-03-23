import soot.ValueBox;

import java.util.Objects;

public class Index {
    private ValueBox index_valuebox;
    private boolean has_index_flag;


    Index(ValueBox index_valuebox) {
        this.index_valuebox = index_valuebox;
        this.has_index_flag = true;
    }
    Index() {
        this.index_valuebox = null;
        this.has_index_flag = false;
    }

    Index(Index i) {
        this.index_valuebox = i.index_valuebox;
        this.has_index_flag = i.has_index_flag;
    }


    String to_str() {
        if(has_index_flag) {
            return index_valuebox.getValue().toString();
        } else {
            return Constants.ARRAY_VERSION_NEW_ARRAY;
        }
    }

    boolean index_set() {
        return has_index_flag;
    }

    boolean equals(Index i) {
        return Objects.equals(i.has_index_flag, has_index_flag) &&
            Objects.equals(to_str(), i.to_str());
    }

}
