package ae2.api.behaviors;

import ae2.api.config.StorageFilter;
import ae2.api.storage.MEStorageChangeListener;

public interface ExternalStorageMonitor {
    void addListener(StorageFilter storageFilter, MEStorageChangeListener listener, Object verificationToken);
    void removeListener(MEStorageChangeListener listener);
}
