package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.block.Gf300Block;
import com.nokosse.ultimaratio.block.entity.Gf300BlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, UltimaRatio.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, UltimaRatio.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, UltimaRatio.MODID);

    public static final DeferredHolder<Block, Block> GF300 = BLOCKS.register("gf300", Gf300Block::new);
    public static final DeferredHolder<Item, Item> GF300_ITEM =
            ITEMS.register("gf300", () -> new BlockItem(GF300.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Gf300BlockEntity>> GF300_BE = BLOCK_ENTITIES.register("gf300",
            () -> BlockEntityType.Builder.of(Gf300BlockEntity::new, GF300.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
