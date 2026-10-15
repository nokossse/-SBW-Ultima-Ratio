package com.nokosse.ultimaratio.inventory;

import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData;
import com.nokosse.ultimaratio.init.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** Menu du FuMO25 (meme ecran radar), seule la validite du bloc change. */
public class Gf300Menu extends FuMO25Menu {
    public Gf300Menu(int id, Inventory inventory, ContainerLevelAccess access, ContainerEnergyData data) {
        super(id, inventory, access, data);
    }

    @Override
    public boolean stillValid(Player player) {
        // 12 blocs : le radar fait 6 de long, le controleur peut etre loin du joueur
        return this.getAccess().evaluate((level, pos) -> level.getBlockState(pos).is(ModBlocks.GF300.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 144, true);
    }
}
