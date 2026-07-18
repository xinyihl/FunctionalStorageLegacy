package com.xinyihl.functionalstoragelegacy.common.integration.ae2;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.MEStorageMonitor;

import com.xinyihl.functionalstoragelegacy.api.storage.BigFluidStack;
import com.xinyihl.functionalstoragelegacy.api.storage.BigItemStack;
import com.xinyihl.functionalstoragelegacy.api.storage.IBigFluidHandler;
import com.xinyihl.functionalstoragelegacy.api.storage.IBigItemHandler;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageAction;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageChange;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageKey;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageSnapshot;
import com.xinyihl.functionalstoragelegacy.api.storage.StorageSubscription;
import com.xinyihl.functionalstoragelegacy.api.storage.TransferResult;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unified Supergiant storage capability for item and fluid drawers.
 *
 * <p>Amounts are aggregated exactly and only saturated at the AE boundary.
 * Once observed, generic drawer changes are translated to the synchronous
 * signed-delta contract introduced by Supergiant PR #71.</p>
 */
public final class DrawerMEStorage implements MEStorageMonitor, AutoCloseable {

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    @Nullable
    private final IBigItemHandler itemHandler;
    @Nullable
    private final IBigFluidHandler fluidHandler;
    private final Map<StorageKey, BigInteger> totals = new HashMap<>();
    private final Map<StorageKey, AEKey> keys = new HashMap<>();
    private final Map<MEStorageChangeListener, ListenerRegistration> listeners = new LinkedHashMap<>();

    private StorageSubscription itemSubscription = StorageSubscription.CLOSED;
    private StorageSubscription fluidSubscription = StorageSubscription.CLOSED;
    private boolean bound;
    private boolean dirty = true;
    private boolean closed;

    public DrawerMEStorage(@Nullable IBigItemHandler itemHandler, @Nullable IBigFluidHandler fluidHandler) {
        this.itemHandler = itemHandler;
        this.fluidHandler = fluidHandler;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (closed || what == null || amount <= 0L) {
            return 0L;
        }
        StorageAction action = mode != null && mode.isSimulate() ? StorageAction.SIMULATE : StorageAction.EXECUTE;
        if (what instanceof AEItemKey && itemHandler != null) {
            ItemStack stack = ((AEItemKey) what).getReadOnlyStack();
            if (stack == null || stack.isEmpty()) {
                return 0L;
            }
            TransferResult<?, ?> result = itemHandler.insertRouted(new BigItemStack(stack, amount), action);
            return clampProcessed(result.getProcessedAmount(), amount);
        }
        if (what instanceof AEFluidKey && fluidHandler != null) {
            FluidStack stack = ((AEFluidKey) what).getReadOnlyStack();
            if (stack == null || stack.getFluid() == null) {
                return 0L;
            }
            TransferResult<?, ?> result = fluidHandler.fillRouted(new BigFluidStack(stack, amount), action);
            return clampProcessed(result.getProcessedAmount(), amount);
        }
        return 0L;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (closed || what == null || amount <= 0L) {
            return 0L;
        }
        StorageAction action = mode != null && mode.isSimulate() ? StorageAction.SIMULATE : StorageAction.EXECUTE;
        if (what instanceof AEItemKey && itemHandler != null) {
            ItemStack stack = ((AEItemKey) what).getReadOnlyStack();
            if (stack == null || stack.isEmpty()) {
                return 0L;
            }
            TransferResult<?, ?> result = itemHandler.extractRouted(new BigItemStack(stack, amount), action);
            return clampProcessed(result.getProcessedAmount(), amount);
        }
        if (what instanceof AEFluidKey && fluidHandler != null) {
            FluidStack stack = ((AEFluidKey) what).getReadOnlyStack();
            if (stack == null || stack.getFluid() == null) {
                return 0L;
            }
            TransferResult<?, ?> result = fluidHandler.drainRouted(new BigFluidStack(stack, amount), action);
            return clampProcessed(result.getProcessedAmount(), amount);
        }
        return 0L;
    }

    @Override
    public synchronized void getAvailableStacks(KeyCounter out) {
        if (closed || out == null) {
            return;
        }
        if (!bound || dirty) {
            scanStorage();
        }
        for (Map.Entry<StorageKey, BigInteger> entry : totals.entrySet()) {
            long amount = saturate(entry.getValue());
            AEKey key = keys.get(entry.getKey());
            if (key != null && amount > 0L) {
                out.add(key, amount);
            }
        }
    }

    @Override
    public ITextComponent getDescription() {
        return new TextComponentString("Functional Storage");
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        if (what instanceof AEItemKey && itemHandler != null) {
            ItemStack stack = ((AEItemKey) what).getReadOnlyStack();
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            BigItemStack request = new BigItemStack(stack, 1L);
            for (int i = 0; i < itemHandler.getStorageCount(); i++) {
                BigItemStack stored = itemHandler.getSnapshot(i);
                if (stored.hasTemplate() && stored.isSameType(request)) {
                    return true;
                }
            }
        } else if (what instanceof AEFluidKey && fluidHandler != null) {
            FluidStack stack = ((AEFluidKey) what).getReadOnlyStack();
            if (stack == null || stack.getFluid() == null) {
                return false;
            }
            BigFluidStack request = new BigFluidStack(stack, 1L);
            for (int i = 0; i < fluidHandler.getStorageCount(); i++) {
                BigFluidStack stored = fluidHandler.getSnapshot(i);
                if (stored.hasTemplate() && stored.isSameType(request)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void addListener(MEStorageChangeListener listener, Object verificationToken) {
        if (closed || listener == null) {
            return;
        }
        if (listeners.containsKey(listener)) {
            throw new IllegalStateException("The storage listener is already registered.");
        }
        listeners.put(listener, new ListenerRegistration(listener, verificationToken));
        if (!bound) {
            bindSources();
        }
    }

    @Override
    public synchronized void removeListener(MEStorageChangeListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            unbindSources();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        listeners.clear();
        unbindSources();
        totals.clear();
        keys.clear();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    private void bindSources() {
        scanStorage();
        if (itemHandler != null) {
            itemSubscription = itemHandler.subscribe(this::acceptItemChange);
        }
        if (fluidHandler != null) {
            fluidSubscription = fluidHandler.subscribe(this::acceptFluidChange);
        }
        bound = true;
    }

    private void unbindSources() {
        itemSubscription.close();
        fluidSubscription.close();
        itemSubscription = StorageSubscription.CLOSED;
        fluidSubscription = StorageSubscription.CLOSED;
        bound = false;
        dirty = true;
    }

    private synchronized void acceptItemChange(StorageChange<BigItemStack, ?> change) {
        acceptChange(change);
    }

    private synchronized void acceptFluidChange(StorageChange<BigFluidStack, ?> change) {
        acceptChange(change);
    }

    private void acceptChange(StorageChange<?, ?> change) {
        if (closed || !bound || change == null) {
            return;
        }
        if (change.isReset()) {
            scanStorage();
            notifyListUpdate();
            return;
        }

        Set<StorageKey> affected = new LinkedHashSet<>();
        for (StorageChange.Entry<?, ?> entry : change.getEntries()) {
            collectKey(entry.getBefore(), affected);
            collectKey(entry.getAfter(), affected);
        }
        Map<StorageKey, Long> before = new HashMap<>();
        for (StorageKey key : affected) {
            before.put(key, saturate(totals.get(key)));
        }
        for (StorageChange.Entry<?, ?> entry : change.getEntries()) {
            applySnapshot(entry.getBefore(), -1);
            applySnapshot(entry.getAfter(), 1);
        }
        for (StorageKey key : affected) {
            long oldAmount = before.get(key);
            long newAmount = saturate(totals.get(key));
            long delta = newAmount - oldAmount;
            AEKey aeKey = keys.get(key);
            if (aeKey != null && delta != 0L) {
                notifyDelta(aeKey, delta);
            }
        }
    }

    private void scanStorage() {
        totals.clear();
        keys.clear();
        if (itemHandler != null) {
            for (int i = 0; i < itemHandler.getStorageCount(); i++) {
                addSnapshot(itemHandler.getSnapshot(i));
            }
        }
        if (fluidHandler != null) {
            for (int i = 0; i < fluidHandler.getStorageCount(); i++) {
                addSnapshot(fluidHandler.getSnapshot(i));
            }
        }
        dirty = false;
    }

    private void addSnapshot(StorageSnapshot<?, ?> snapshot) {
        if (snapshot == null || !snapshot.hasTemplate() || snapshot.getAmount() <= 0L) {
            return;
        }
        StorageKey storageKey = snapshot.getKey();
        AEKey aeKey = toAEKey(snapshot);
        if (storageKey == null || aeKey == null) {
            return;
        }
        keys.put(storageKey, aeKey);
        BigInteger amount = BigInteger.valueOf(snapshot.getAmount());
        BigInteger previous = totals.get(storageKey);
        totals.put(storageKey, previous == null ? amount : previous.add(amount));
    }

    private void collectKey(Object rawSnapshot, Set<StorageKey> affected) {
        if (rawSnapshot instanceof StorageSnapshot) {
            StorageKey key = ((StorageSnapshot<?, ?>) rawSnapshot).getKey();
            if (key != null) {
                affected.add(key);
            }
        }
    }

    private void applySnapshot(Object rawSnapshot, int sign) {
        if (!(rawSnapshot instanceof StorageSnapshot)) {
            return;
        }
        StorageSnapshot<?, ?> snapshot = (StorageSnapshot<?, ?>) rawSnapshot;
        StorageKey key = snapshot.getKey();
        if (key == null) {
            return;
        }
        AEKey aeKey = toAEKey(snapshot);
        if (aeKey != null) {
            keys.put(key, aeKey);
        }
        BigInteger amount = BigInteger.valueOf(Math.max(0L, snapshot.getAmount()));
        BigInteger current = totals.get(key);
        BigInteger updated = (current == null ? BigInteger.ZERO : current)
                .add(sign < 0 ? amount.negate() : amount);
        if (updated.signum() <= 0) {
            totals.remove(key);
        } else {
            totals.put(key, updated);
        }
    }

    @Nullable
    private static AEKey toAEKey(StorageSnapshot<?, ?> snapshot) {
        if (snapshot instanceof BigItemStack) {
            BigItemStack item = (BigItemStack) snapshot;
            return item.hasTemplate() ? AEItemKey.of(item.getTemplate()) : null;
        }
        if (snapshot instanceof BigFluidStack) {
            BigFluidStack fluid = (BigFluidStack) snapshot;
            return fluid.hasTemplate() ? AEFluidKey.of(fluid.getTemplate()) : null;
        }
        return null;
    }

    private void notifyDelta(AEKey key, long delta) {
        for (ListenerRegistration registration : validListeners()) {
            registration.listener.onStackChange(key, delta);
        }
        unbindIfUnobserved();
    }

    private void notifyListUpdate() {
        for (ListenerRegistration registration : validListeners()) {
            registration.listener.onListUpdate();
        }
        unbindIfUnobserved();
    }

    private List<ListenerRegistration> validListeners() {
        List<ListenerRegistration> valid = new ArrayList<>();
        List<MEStorageChangeListener> stale = new ArrayList<>();
        for (ListenerRegistration registration : listeners.values()) {
            if (registration.listener.isValid(registration.verificationToken)) {
                valid.add(registration);
            } else {
                stale.add(registration.listener);
            }
        }
        for (MEStorageChangeListener listener : stale) {
            listeners.remove(listener);
        }
        return valid;
    }

    private void unbindIfUnobserved() {
        if (listeners.isEmpty()) {
            unbindSources();
        }
    }

    private static long saturate(@Nullable BigInteger amount) {
        if (amount == null || amount.signum() <= 0) {
            return 0L;
        }
        return amount.compareTo(LONG_MAX) > 0 ? Long.MAX_VALUE : amount.longValue();
    }

    private static long clampProcessed(long processed, long requested) {
        return Math.min(requested, Math.max(0L, processed));
    }

    private static final class ListenerRegistration {
        private final MEStorageChangeListener listener;
        private final Object verificationToken;

        private ListenerRegistration(MEStorageChangeListener listener, Object verificationToken) {
            this.listener = listener;
            this.verificationToken = verificationToken;
        }
    }
}
