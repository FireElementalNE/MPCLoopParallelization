/**
 * Hash class for Edges
 */

@SuppressWarnings({"FieldCanBeLocal", "unused"})
class DefUseHash {
    /**
     * definition string
     */
    private final String def;
    /**
     * use string
     */
    private final String use;

    /**
     * @param def the definition as a string
     * @param use the use as a string
     */
    DefUseHash(String def, String use) {
        this.def = def;
        this.use = use;
    }
}
