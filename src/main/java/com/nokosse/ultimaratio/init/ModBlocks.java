package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.block.Gf300Block;
import com.nokosse.ultimaratio.block.entity.Gf300BlockEntity;
import com.nokosse.ultimaratio.block.GcsBlock;
import com.nokosse.ultimaratio.block.entity.GcsBlockEntity;
import com.nokosse.ultimaratio.item.VsrMonitorItem;
import com.nokosse.ultimaratio.item.DescribedBlockItem;
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
            ITEMS.register("gf300", () -> new DescribedBlockItem(GF300.get(), new Item.Properties(), "gf300", 3));
    public static final RegistryObject<Item> VSR_MONITOR = ITEMS.register("vsr_monitor", VsrMonitorItem::new);
    public static final RegistryObject<Block> GCS = BLOCKS.register("gcs", GcsBlock::new);
    public static final RegistryObject<Item> GCS_ITEM =
            ITEMS.register("gcs", () -> new DescribedBlockItem(GCS.get(), new Item.Properties().stacksTo(1), "gcs", 3));
    public static final RegistryObject<BlockEntityType<GcsBlockEntity>> GCS_BE = BLOCK_ENTITIES.register("gcs",
            () -> BlockEntityType.Builder.of(GcsBlockEntity::new, GCS.get()).build(null));
    public static final RegistryObject<BlockEntityType<Gf300BlockEntity>> GF300_BE = BLOCK_ENTITIES.register("gf300",
            () -> BlockEntityType.Builder.of(Gf300BlockEntity::new, GF300.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
