package ae2.api.storage;

import ae2.api.stacks.AEKey;

public interface MEStorageChangeListener {
    boolean isValid(Object verificationToken);
    void onStackChange(AEKey what, long delta);
    void onListUpdate();
}
