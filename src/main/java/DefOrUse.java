/**
 * Enum representing whether a Node is a Definition Node or a Usage Node.
 */
public enum DefOrUse {
    DEF("DEF"),
    USE("USE");

    private final String text;

    /**
     * @param text convert to string
     */
    DefOrUse(final String text) {
        this.text = text;
    }
    @Override
    public String toString() {
        return text;
    }
}
