package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.inventory.Mq9Menu;
import com.nokosse.ultimaratio.inventory.Vsr700Menu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, UltimaRatio.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<Vsr700Menu>> VSR700 = MENUS.register("vsr700",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new Vsr700Menu(id, inventory, buf.readInt())));

    public static final DeferredHolder<MenuType<?>, MenuType<Mq9Menu>> MQ9 = MENUS.register("mq9_reaper",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new Mq9Menu(id, inventory, buf.readInt())));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
