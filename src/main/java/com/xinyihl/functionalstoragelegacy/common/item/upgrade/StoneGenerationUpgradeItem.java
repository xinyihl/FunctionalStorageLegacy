package com.xinyihl.functionalstoragelegacy.common.item.upgrade;

import com.xinyihl.functionalstoragelegacy.common.inventory.CompactingInventoryHandler;
import com.xinyihl.functionalstoragelegacy.common.tile.base.ControllableDrawerTile;
import com.xinyihl.functionalstoragelegacy.misc.Configurations;
import com.xinyihl.functionalstoragelegacy.misc.RegistrationHandler;
import com.xinyihl.functionalstoragelegacy.util.CompactingUtil;
import com.xinyihl.functionalstoragelegacy.util.TimerUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public class StoneGenerationUpgradeItem extends UtilityUpgradeItem {

    private final StoneTier tier;

    public StoneGenerationUpgradeItem(StoneTier tier) {
        super(UtilityAction.NONE);
        this.setCreativeTab(RegistrationHandler.CREATIVE_TAB);
        this.setMaxStackSize(1);
        this.tier = tier;
    }

    @Override
    public void onTick(ControllableDrawerTile tile, ItemStack upgradeStack, int upgradeSlot) {

        if (tile.getWorld().isRemote) {
            return;
        }

        if (!upgradeStack.hasTagCompound()) {
            upgradeStack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = upgradeStack.getTagCompound();

        TimerUtil.updateAndExecute(nbt, getGenerationInterval(), () -> GenerationTreatment(tile));
    }

    private int getGenerationInterval() {
        return (int) Math.ceil(20.0 / tier.getGenerationRate());
    }


    public static boolean GenerationTreatment(ControllableDrawerTile tile) {
        IItemHandler handler = tile.getItemHandler();
        if (handler == null) {
            return false;
        } else {
            if (handler instanceof CompactingInventoryHandler) {
                CompactingInventoryHandler compactingHandler = (CompactingInventoryHandler) handler;
                if (!compactingHandler.isSetup()) {
                    ItemStack stoneStack = new ItemStack(Blocks.COBBLESTONE, 1);
                    if (CompactingUtil.CompressionDrawertrEatment(tile, stoneStack, compactingHandler)) {
                        return false;
                    }
                }
            }

            ItemStack stoneStack = new ItemStack(Blocks.COBBLESTONE, 1);
            return CompactingUtil.ItemRemainder(tile, handler, stoneStack);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        String rateText = String.format("%.1f/s", tier.getGenerationRate());
        tooltip.add(TextFormatting.YELLOW + I18n.format("item.functionalstoragelegacy.generation_upgrade.rate") + TextFormatting.WHITE + rateText);

    }

    public enum StoneTier {
        T1(1),
        T2(2),
        T3(3),
        T4(4);

        private final int tier;

        StoneTier(int tier) {
            this.tier = tier;
        }

        public float getGenerationRate() {
            switch (this) {
                case T2:
                    return Configurations.GENERATION.stoneGenerationT2;
                case T3:
                    return Configurations.GENERATION.stoneGenerationT3;
                case T4:
                    return Configurations.GENERATION.stoneGenerationT4;
                case T1:
                default:
                    return Configurations.GENERATION.stoneGenerationT1;
            }
        }

        public int getTier() {
            return tier;
        }
    }
}