package com.nokosse.ultimaratio.inventory;

import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.nokosse.ultimaratio.init.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** The FuMO-25's menu (same radar screen); only the block it stays valid for changes. */
public class Gf300Menu extends FuMO25Menu {
    public Gf300Menu(int id, Inventory inventory, ContainerLevelAccess access, ContainerEnergyData data) {
        super(id, inventory, access, data);
    }

    @Override
    public boolean stillValid(Player player) {
        // 12 blocks: the radar is 6 long, so the controller cell can be far from the player
        return this.getAccess().evaluate((level, pos) -> level.getBlockState(pos).is(ModBlocks.GF300.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 144, true);
    }
}
