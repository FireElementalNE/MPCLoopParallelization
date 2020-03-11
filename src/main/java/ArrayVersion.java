interface ArrayVersion {
    boolean is_phi();
    Index get_index();
    int get_version();
    void incr_version();
    boolean has_diff_ver_match();
}
