package ae2.api.config;

public enum Actionable {
    MODULATE,
    SIMULATE;

    public boolean isSimulate() {
        return this == SIMULATE;
    }
}
