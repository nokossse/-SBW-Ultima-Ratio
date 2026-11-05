package com.nokosse.ultimaratio.inventory;

import com.atsuishio.superbwarfare.inventory.menu.AbstractVehicleContainerMenu;
import com.nokosse.ultimaratio.init.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

/** The VSR700's single-slot magazine menu: one cell for its Mistral missiles, plus the player inventory. */
public class Vsr700Menu extends AbstractVehicleContainerMenu {
    public Vsr700Menu(int id, Inventory inventory, int entityId) {
        this(ModMenus.VSR700.get(), id, inventory, entityId);
    }

    protected Vsr700Menu(MenuType<?> type, int id, Inventory inventory, int entityId) {
        super(type, id, inventory, entityId);
    }

    /** Number of vehicle cells in the row (the screen draws as many). */
    public int cells() {
        return 1;
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
