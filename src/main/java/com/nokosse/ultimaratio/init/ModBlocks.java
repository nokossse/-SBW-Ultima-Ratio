package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.block.Gf300Block;
import com.nokosse.ultimaratio.block.entity.Gf300BlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, UltimaRatio.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UltimaRatio.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, UltimaRatio.MODID);

    public static final RegistryObject<Block> GF300 = BLOCKS.register("gf300", Gf300Block::new);
    public static final RegistryObject<Item> GF300_ITEM =
            ITEMS.register("gf300", () -> new BlockItem(GF300.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<Gf300BlockEntity>> GF300_BE = BLOCK_ENTITIES.register("gf300",
            () -> BlockEntityType.Builder.of(Gf300BlockEntity::new, GF300.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
