package ae2.api.stacks;

import java.util.LinkedHashMap;
import java.util.Map;

public class KeyCounter {
    private final Map<AEKey, Long> amounts = new LinkedHashMap<>();
    private final boolean saturating;

    public KeyCounter() {
        this(false);
    }

    private KeyCounter(boolean saturating) {
        this.saturating = saturating;
    }

    public static KeyCounter saturating() {
        return new KeyCounter(true);
    }

    public void add(AEKey key, long amount) {
        long current = get(key);
        long updated;
        if (saturating && amount > 0L && current > Long.MAX_VALUE - amount) {
            updated = Long.MAX_VALUE;
        } else {
            updated = current + amount;
        }
        if (updated == 0L) amounts.remove(key); else amounts.put(key, updated);
    }

    public long get(AEKey key) { return amounts.containsKey(key) ? amounts.get(key) : 0L; }
    public int size() { return amounts.size(); }
}
