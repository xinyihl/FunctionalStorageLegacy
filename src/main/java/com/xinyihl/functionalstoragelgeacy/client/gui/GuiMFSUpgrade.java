package com.xinyihl.functionalstoragelgeacy.client.gui;

import com.xinyihl.functionalstoragelgeacy.container.ContainerMFSUpgrade;
import com.xinyihl.functionalstoragelgeacy.item.MFSUpgradeItem;
import com.xinyihl.functionalstoragelgeacy.item.RefillUpgradeItem;
import com.xinyihl.functionalstoragelgeacy.network.NetworkHandler;
import com.xinyihl.functionalstoragelgeacy.network.PacketMFSUpgradeAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiMFSUpgrade extends GuiContainer {

    private final ContainerMFSUpgrade container;
    private GuiButton directionButton;
    private GuiButton redstoneButton;
    private GuiButton refillButton;
    private GuiButton blacklistButton;
    private GuiButton matchNbtButton;
    private GuiButton backButton;
    private final List<GuiButton> slotButtons = new ArrayList<>();

    public GuiMFSUpgrade(ContainerMFSUpgrade inventorySlotsIn) {
        super(inventorySlotsIn);
        this.container = inventorySlotsIn;
        this.xSize = 176;
        this.ySize = 194;
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        slotButtons.clear();

        directionButton = addButton(new GuiButton(0, guiLeft + 60, guiTop + 6, 72, 20, ""));
        redstoneButton = addButton(new GuiButton(1, guiLeft + 60, guiTop + 28, 108, 20, ""));
        refillButton = addButton(new GuiButton(2, guiLeft + 60, guiTop + 50, 108, 20, ""));
        blacklistButton = addButton(new GuiButton(3, guiLeft + 60, guiTop + 72, 52, 20, ""));
        matchNbtButton = addButton(new GuiButton(4, guiLeft + 116, guiTop + 72, 52, 20, ""));
        backButton = addButton(new GuiButton(5, guiLeft + 136, guiTop + 94, 32, 16, I18n.format("gui.functionalstoragelgeacy.back")));

        for (int i = 0; i < 4; i++) {
            GuiButton button = addButton(new GuiButton(10 + i, guiLeft + 60 + i * 19, guiTop + 94, 18, 16, ""));
            slotButtons.add(button);
        }
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (!button.enabled) {
            return;
        }

        PacketMFSUpgradeAction.Action action = null;
        int value = 0;

        if (button.id == 0) {
            action = PacketMFSUpgradeAction.Action.CYCLE_DIRECTION;
        } else if (button.id == 1) {
            action = PacketMFSUpgradeAction.Action.CYCLE_REDSTONE;
        } else if (button.id == 2) {
            action = PacketMFSUpgradeAction.Action.CYCLE_REFILL_TARGET;
        } else if (button.id == 3) {
            action = PacketMFSUpgradeAction.Action.TOGGLE_BLACKLIST;
        } else if (button.id == 4) {
            action = PacketMFSUpgradeAction.Action.TOGGLE_MATCH_NBT;
        } else if (button.id == 5) {
            action = PacketMFSUpgradeAction.Action.OPEN_DRAWER_GUI;
        } else if (button.id >= 10 && button.id < 14) {
            action = PacketMFSUpgradeAction.Action.TOGGLE_SELECTED_SLOT;
            value = button.id - 10;
        }

        if (action != null) {
            NetworkHandler.CHANNEL.sendToServer(new PacketMFSUpgradeAction(container.getTile().getPos(), container.getUpgradeSlot(), action, value));
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateButtons();
    }

    private void updateButtons() {
        ItemStack stack = container.getUpgradeStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof MFSUpgradeItem)) {
            return;
        }

        MFSUpgradeItem upgrade = (MFSUpgradeItem) stack.getItem();
        directionButton.visible = upgrade.hasDirection();
        directionButton.displayString = I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.direction")
                + ": "
                + I18n.format(MFSUpgradeItem.getRelativeDirection(stack).getTranslationKey());

        redstoneButton.displayString = I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.redstone")
                + ": "
                + I18n.format(MFSUpgradeItem.getRedstoneMode(stack).getTranslationKey());

        refillButton.visible = upgrade instanceof RefillUpgradeItem;
        if (refillButton.visible) {
            refillButton.displayString = I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.refill_target")
                    + ": "
                    + I18n.format(RefillUpgradeItem.getRefillTarget(stack).name().equals("HOTBAR")
                    ? "gui.functionalstoragelgeacy.mfs_upgrade.refill.hotbar"
                    : RefillUpgradeItem.getRefillTarget(stack).name().equals("INVENTORY")
                    ? "gui.functionalstoragelgeacy.mfs_upgrade.refill.inventory"
                    : "gui.functionalstoragelgeacy.mfs_upgrade.refill.ender");
        }

        blacklistButton.displayString = I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.blacklist",
                MFSUpgradeItem.getFilterConfiguration(stack).isBlacklist() ? I18n.format("options.on") : I18n.format("options.off"));
        matchNbtButton.displayString = I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.match_nbt",
                MFSUpgradeItem.getFilterConfiguration(stack).isMatchNBT() ? I18n.format("options.on") : I18n.format("options.off"));

        int selectableSlots = container.getTile().getItemHandler() == null ? 0 : Math.min(4, container.getTile().getItemHandler().getSlots());
        List<Integer> selectedSlots = MFSUpgradeItem.getSelectedSlots(stack, selectableSlots);
        for (int i = 0; i < slotButtons.size(); i++) {
            GuiButton button = slotButtons.get(i);
            button.visible = i < selectableSlots;
            button.displayString = selectedSlots.contains(i) ? "[" + (i + 1) + "]" : String.valueOf(i + 1);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.title"), 8, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 100, 4210752);
        fontRenderer.drawString(I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.augment"), 8, 6 + 20, 4210752);
        fontRenderer.drawString(I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.tool"), 30, 6 + 20, 4210752);
        fontRenderer.drawString(I18n.format("gui.functionalstoragelgeacy.mfs_upgrade.filters"), 8, 34, 4210752);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawGradientRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFFBFBFBF, 0xFF9F9F9F);
        drawGradientRect(guiLeft + 7, guiTop + 17, guiLeft + 25, guiTop + 35, 0xFF4A4A4A, 0xFF3A3A3A);
        drawGradientRect(guiLeft + 29, guiTop + 17, guiLeft + 47, guiTop + 35, 0xFF4A4A4A, 0xFF3A3A3A);
        for (int i = 0; i < ContainerMFSUpgrade.FILTER_COUNT; i++) {
            int slotX = guiLeft + 7 + (i % 3) * 18;
            int slotY = guiTop + 43 + (i / 3) * 18;
            drawGradientRect(slotX, slotY, slotX + 18, slotY + 18, 0xFF4A4A4A, 0xFF3A3A3A);
        }
        drawGradientRect(guiLeft + 7, guiTop + 111, guiLeft + xSize - 7, guiTop + ySize - 7, 0x22000000, 0x22000000);
    }
}
