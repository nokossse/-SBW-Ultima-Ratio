package com.nokosse.ultimaratio.client.screen;

import com.atsuishio.superbwarfare.client.screens.AbstractVehicleContainerScreen;
import com.nokosse.ultimaratio.inventory.Vsr700Menu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** One-slot menu screen: SBW's player inventory texture under a plain panel holding the single cell. */
public class Vsr700Screen<M extends Vsr700Menu> extends AbstractVehicleContainerScreen<M> {
    public Vsr700Screen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // Panel (vanilla-style bevel) over the top 41 px, player inventory texture below
        graphics.fill(x, y, x + 176, y + 41, 0xFFC6C6C6);
        graphics.fill(x, y, x + 176, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + 41, 0xFFFFFFFF);
        graphics.fill(x, y + 40, x + 176, y + 41, 0xFF555555);
        graphics.fill(x + 175, y, x + 176, y + 41, 0xFF555555);
        // The cells, centred on x=88 (the first slot at (80,18) for one): dark top/left edges, light bottom/right edges, grey inside
        for (int i = 0; i < this.menu.cells(); i++) {
            int cx = x + 79 - (this.menu.cells() - 1) * 9 + 18 * i;
            int cy = y + 17;
            graphics.fill(cx, cy, cx + 18, cy + 18, 0xFFFFFFFF);
            graphics.fill(cx, cy, cx + 17, cy + 17, 0xFF373737);
            graphics.fill(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF8B8B8B);
        }
        graphics.blit(Companion.getINV(), x, y + 23 + this.menu.getRows() * 18, 0, 0, 176, 90);
    }
}
