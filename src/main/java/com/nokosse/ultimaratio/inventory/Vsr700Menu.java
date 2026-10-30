package com.nokosse.ultimaratio.inventory;

import com.atsuishio.superbwarfare.inventory.menu.AbstractVehicleContainerMenu;
import com.nokosse.ultimaratio.init.ModMenus;
import net.minecraft.world.entity.player.Inventory;

/** The VSR700's single-slot magazine menu: one cell for its Kh-39, plus the player inventory. */
public class Vsr700Menu extends AbstractVehicleContainerMenu {
    public Vsr700Menu(int id, Inventory inventory, int entityId) {
        super(ModMenus.VSR700.get(), id, inventory, entityId);
    }

    /** One row of vehicle slots in SBW's layout maths (player inventory sits below it). */
    @Override
    public int getRows() {
        return 1;
    }

    @Override
    public void addVehicleInventory() {
        this.addSlot(new VehicleSlot(this.getVehicle(), 0, 80, 18));
    }
}
