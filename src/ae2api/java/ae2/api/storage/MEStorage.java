package ae2.api.storage;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import net.minecraft.util.text.ITextComponent;

public interface MEStorage {
    long insert(AEKey what, long amount, Actionable mode, IActionSource source);
    long extract(AEKey what, long amount, Actionable mode, IActionSource source);
    void getAvailableStacks(KeyCounter out);
    ITextComponent getDescription();
    default boolean isPreferredStorageFor(AEKey what, IActionSource source) { return false; }
    default boolean isStickyStorageFor(AEKey what, IActionSource source) { return false; }
}
