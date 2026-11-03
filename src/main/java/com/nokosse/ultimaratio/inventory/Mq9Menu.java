package com.nokosse.ultimaratio.inventory;

import com.nokosse.ultimaratio.init.ModMenus;
import net.minecraft.world.entity.player.Inventory;

/** The MQ-9's four-cell magazine menu: one row, the other cells of its "Mini" container stay locked. */
public class Mq9Menu extends Vsr700Menu {
    public Mq9Menu(int id, Inventory inventory, int entityId) {
        super(ModMenus.MQ9.get(), id, inventory, entityId);
    }

    @Override
    public int cells() {
        return 4;
    }

    @Override
    public void addVehicleInventory() {
        for (int i = 0; i < cells(); i++) {
            this.addSlot(new VehicleSlot(this.getVehicle(), i, 53 + 18 * i, 18));
        }
    }
}
