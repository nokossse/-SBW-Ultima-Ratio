package com.nokosse.ultimaratio.client.screen;

import net.neoforged.neoforge.network.PacketDistributor;
import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen;
import com.nokosse.ultimaratio.network.GcsListMessage;
import com.nokosse.ultimaratio.network.GcsRefreshMessage;
import com.nokosse.ultimaratio.network.GcsSelectMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/** The GCS's drone list: one button per drone of the player's side, nearest first. */
public class GcsScreen extends Screen {
    // shortcut: no scrolling, drones past the 10th are not listed; add a scroll list if a side ever fields more
    private static final int MAX_ROWS = 10;
    private final BlockPos pos;
    private List<GcsListMessage.Entry> drones;
    private int refreshTicks;

    private GcsScreen(BlockPos pos, List<GcsListMessage.Entry> drones) {
        super(Component.translatable("gui.ultimaratio.gcs.title"));
        this.pos = pos;
        this.drones = drones;
    }

    /** A list from the server: refreshes the screen already open on this desk, else opens one. */
    public static void open(BlockPos pos, List<GcsListMessage.Entry> drones) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GcsScreen open && open.pos.equals(pos)) {
            open.drones = drones;
            open.rebuildWidgets();
        } else {
            mc.setScreen(new GcsScreen(pos, drones));
        }
    }

    @Override
    public void tick() {
        if (++refreshTicks % 20 == 0) {
            PacketDistributor.sendToServer(new GcsRefreshMessage(pos));
        }
    }

    @Override
    protected void init() {
        int top = height / 2 - Math.min(drones.size(), MAX_ROWS) * 12;
        for (int i = 0; i < Math.min(drones.size(), MAX_ROWS); i++) {
            GcsListMessage.Entry drone = drones.get(i);
            addRenderableWidget(Button.builder(Component.literal(drone.name() + "  -  " + drone.distance() + " m"), b -> {
                PacketDistributor.sendToServer(new GcsSelectMessage(pos, drone.drone()));
                onClose();
            }).bounds(width / 2 - 100, top + i * 24, 200, 20).build());
        }
        // the GCS has its own map: no tactical terminal needed
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimaratio.gcs.map"),
                b -> Minecraft.getInstance().setScreen(new TacticalMapScreen()))
                .bounds(width / 2 - 100, top + Math.min(drones.size(), MAX_ROWS) * 24 + 8, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); // 1.21: draws the background, then the buttons
        int top = height / 2 - Math.min(drones.size(), MAX_ROWS) * 12;
        graphics.drawCenteredString(font, title, width / 2, top - 20, 0xFFFFFF);
        if (drones.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.ultimaratio.gcs.none"), width / 2, height / 2, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
