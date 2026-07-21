package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import ae2.api.config.Actionable;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorageChangeListener;
import com.xinyihl.functionalstoragelegacy.api.storage.BigItemStack;
import com.xinyihl.functionalstoragelegacy.api.storage.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.api.storage.ItemStorageKey;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageAction;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageChange;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageChangeDispatcher;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageSubscription;
import com.xinyihl.functionalstoragelegacy.api.storage.TransferResult;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DrawerMEStorageTest {

    @BeforeAll
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void insertAndExtractKeepLongAmountsAndSimulationSemantics() {
        Item item = new Item();
        MutableItemHandler handler = new MutableItemHandler(item, 7L, 0L);
        DrawerMEStorage storage = new DrawerMEStorage(handler, null);
        AEItemKey key = AEItemKey.of(new ItemStack(item));

        assertEquals(7L, storage.insert(key, 10L, Actionable.SIMULATE, null));
        assertEquals(0L, handler.stored);
        assertEquals(7L, storage.insert(key, 10L, Actionable.MODULATE, null));
        assertEquals(7L, handler.stored);

        assertEquals(7L, storage.extract(key, 10L, Actionable.SIMULATE, null));
        assertEquals(7L, handler.stored);
        assertEquals(4L, storage.extract(key, 4L, Actionable.MODULATE, null));
        assertEquals(3L, handler.stored);
    }

    @Test
    public void availableStacksAggregateDuplicateSlotsWithSaturation() {
        Item first = new Item();
        Item second = new Item();
        SnapshotItemHandler handler = new SnapshotItemHandler(
                new BigItemStack(new ItemStack(first), Long.MAX_VALUE),
                new BigItemStack(new ItemStack(first), Long.MAX_VALUE),
                new BigItemStack(new ItemStack(second), 9L));
        DrawerMEStorage storage = new DrawerMEStorage(handler, null);
        KeyCounter counter = KeyCounter.saturating();

        storage.getAvailableStacks(counter);

        assertEquals(2, counter.size());
        assertEquals(Long.MAX_VALUE, counter.get(AEItemKey.of(new ItemStack(first))));
        assertEquals(9L, counter.get(AEItemKey.of(new ItemStack(second))));
    }

    @Test
    public void monitorPublishesSignedDeltasAndStructuralReset() {
        Item item = new Item();
        MutableItemHandler handler = new MutableItemHandler(item, 30L, 10L);
        DrawerMEStorage storage = new DrawerMEStorage(handler, null);
        RecordingListener listener = new RecordingListener();
        storage.addListener(listener, listener);
        AEItemKey key = AEItemKey.of(new ItemStack(item));

        assertEquals(4L, storage.insert(key, 4L, Actionable.MODULATE, null));
        assertEquals(3L, storage.extract(key, 3L, Actionable.MODULATE, null));
        assertEquals(2, listener.deltas.size());
        assertEquals(Long.valueOf(4L), listener.deltas.get(0));
        assertEquals(Long.valueOf(-3L), listener.deltas.get(1));

        handler.onChange(StorageChange.reset());
        assertEquals(1, listener.listUpdates);

        listener.valid = false;
        storage.insert(key, 1L, Actionable.MODULATE, null);
        assertEquals(2, listener.deltas.size());
        assertFalse(storage.isClosed());
    }

    private static final class RecordingListener implements MEStorageChangeListener {
        private final List<Long> deltas = new ArrayList<>();
        private boolean valid = true;
        private int listUpdates;

        @Override
        public boolean isValid(Object verificationToken) {
            return valid && verificationToken == this;
        }

        @Override
        public void onStackChange(ae2.api.stacks.AEKey what, long delta) {
            deltas.add(delta);
        }

        @Override
        public void onListUpdate() {
            listUpdates++;
        }
    }

    private static final class MutableItemHandler implements IBigItemHandler {
        private final Item item;
        private final long capacity;
        private final StorageChangeDispatcher<BigItemStack, ItemStorageKey> dispatcher = new StorageChangeDispatcher<>();
        private long stored;

        private MutableItemHandler(Item item, long capacity, long stored) {
            this.item = item;
            this.capacity = capacity;
            this.stored = stored;
        }

        @Override
        public int getStorageCount() {
            return 1;
        }

        @Nonnull
        @Override
        public BigItemStack getSnapshot(int index) {
            return index == 0 && stored > 0L
                    ? new BigItemStack(new ItemStack(item), stored)
                    : BigItemStack.empty();
        }

        @Override
        public long getCapacity(int index) {
            return index == 0 ? capacity : 0L;
        }

        @Nonnull
        @Override
        public TransferResult<BigItemStack, ItemStorageKey> insert(int index, @Nonnull BigItemStack request, @Nonnull StorageAction action) {
            BigItemStack before = getSnapshot(index);
            long processed = index == 0 && request.isSameType(new ItemStack(item))
                    ? Math.min(request.getAmount(), capacity - stored) : 0L;
            if (action == StorageAction.EXECUTE && processed > 0L) {
                stored += processed;
                onChange(StorageChange.delta(index, before, getSnapshot(index)));
            }
            return new TransferResult<>(request.getAmount(), processed == 0L
                    ? BigItemStack.empty() : request.withAmount(processed), action);
        }

        @Nonnull
        @Override
        public TransferResult<BigItemStack, ItemStorageKey> extract(int index, long amount, @Nonnull StorageAction action) {
            BigItemStack before = getSnapshot(index);
            long processed = index == 0 ? Math.min(Math.max(0L, amount), stored) : 0L;
            if (action == StorageAction.EXECUTE && processed > 0L) {
                stored -= processed;
                onChange(StorageChange.delta(index, before, getSnapshot(index)));
            }
            BigItemStack result = processed == 0L ? BigItemStack.empty()
                    : new BigItemStack(new ItemStack(item), processed);
            return new TransferResult<>(Math.max(0L, amount), result, action);
        }

        @Override
        public void onChange(@Nonnull StorageChange<BigItemStack, ItemStorageKey> change) {
            dispatcher.dispatch(change);
        }

        @Nonnull
        @Override
        public StorageSubscription subscribe(@Nonnull Consumer<? super StorageChange<BigItemStack, ItemStorageKey>> listener) {
            return dispatcher.subscribe(listener);
        }
    }

    private static final class SnapshotItemHandler implements IBigItemHandler {
        private final BigItemStack[] snapshots;

        private SnapshotItemHandler(BigItemStack... snapshots) {
            this.snapshots = snapshots;
        }

        @Override
        public int getStorageCount() {
            return snapshots.length;
        }

        @Nonnull
        @Override
        public BigItemStack getSnapshot(int index) {
            return index < 0 || index >= snapshots.length ? BigItemStack.empty() : snapshots[index];
        }

        @Override
        public long getCapacity(int index) {
            return index < 0 || index >= snapshots.length ? 0L : Long.MAX_VALUE;
        }

        @Nonnull
        @Override
        public TransferResult<BigItemStack, ItemStorageKey> insert(int index, @Nonnull BigItemStack request, @Nonnull StorageAction action) {
            return new TransferResult<>(request.getAmount(), BigItemStack.empty(), action);
        }

        @Nonnull
        @Override
        public TransferResult<BigItemStack, ItemStorageKey> extract(int index, long amount, @Nonnull StorageAction action) {
            return new TransferResult<>(Math.max(0L, amount), BigItemStack.empty(), action);
        }
    }
}
