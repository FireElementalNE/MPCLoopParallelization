/**
 * Hash class for Edges
 */
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
class DefUseHash {
    private String def;
    private String use;

    /**
     * @param def the definition as a string
     * @param use the use as a string
     */
    DefUseHash(String def, String use) {
        this.def = def;
        this.use = use;
    }
}
