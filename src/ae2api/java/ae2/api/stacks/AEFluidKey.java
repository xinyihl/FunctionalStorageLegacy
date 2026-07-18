package ae2.api.stacks;

import net.minecraftforge.fluids.FluidStack;

import java.util.Objects;

public final class AEFluidKey extends AEKey {
    private final FluidStack stack;

    private AEFluidKey(FluidStack stack) {
        this.stack = stack.copy();
        this.stack.amount = 1;
    }

    public static AEFluidKey of(FluidStack stack) {
        return stack == null || stack.getFluid() == null || stack.amount <= 0 ? null : new AEFluidKey(stack);
    }
    public static boolean is(AEKey key) { return key instanceof AEFluidKey; }
    public FluidStack getReadOnlyStack() { return stack.copy(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AEFluidKey)) return false;
        FluidStack that = ((AEFluidKey) other).stack;
        return stack.getFluid() == that.getFluid() && Objects.equals(stack.tag, that.tag);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(stack.getFluid()) + (stack.tag == null ? 0 : stack.tag.hashCode());
    }
}
