public enum DefOrUse {
    DEF("DEF"),
    USE("USE");

    private final String text;
    DefOrUse(final String text) {
        this.text = text;
    }
    @Override
    public String toString() {
        return text;
    }
}
