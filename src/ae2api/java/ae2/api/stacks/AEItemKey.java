package ae2.api.stacks;

import net.minecraft.item.ItemStack;

public final class AEItemKey extends AEKey {
    private final ItemStack stack;

    private AEItemKey(ItemStack stack) {
        this.stack = stack.copy();
        this.stack.setCount(1);
    }

    public static AEItemKey of(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : new AEItemKey(stack);
    }
    public static boolean is(AEKey key) { return key instanceof AEItemKey; }
    public ItemStack getReadOnlyStack() { return stack.copy(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AEItemKey)) return false;
        ItemStack that = ((AEItemKey) other).stack;
        return ItemStack.areItemsEqual(stack, that) && ItemStack.areItemStackTagsEqual(stack, that);
    }

    @Override
    public int hashCode() {
        int result = 31 * System.identityHashCode(stack.getItem()) + stack.getMetadata();
        return 31 * result + (stack.getTagCompound() == null ? 0 : stack.getTagCompound().hashCode());
    }
}
