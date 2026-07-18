package ae2.api.storage;

public interface MEStorageMonitor extends MEStorage {
    void addListener(MEStorageChangeListener listener, Object verificationToken);
    void removeListener(MEStorageChangeListener listener);
}
