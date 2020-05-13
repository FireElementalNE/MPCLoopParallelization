/**
 * Enum representing whether an SCC node is a Definition Node or a Usage Node.
 */
public enum ReadWrite {
    READ("READ"),
    WRITE("WRITE");

    private final String text;

    /**
     * @param text convert to string
     */
    ReadWrite(final String text) {
        this.text = text;
    }
    @Override
    public String toString() {
        return text;
    }
}
