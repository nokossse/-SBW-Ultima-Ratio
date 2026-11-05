package com.nokosse.ultimaratio.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A block item whose tooltip explains how to use the block: lines des.ultimaratio.&lt;id&gt;.1 to .lines. */
public class DescribedBlockItem extends BlockItem {
    private final String key;
    private final int lines;

    public DescribedBlockItem(Block block, Properties properties, String id, int lines) {
        super(block, properties);
        this.key = "des.ultimaratio." + id + ".";
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int i = 1; i <= this.lines; i++) {
            tooltip.add(Component.translatable(this.key + i).withStyle(ChatFormatting.GRAY));
        }
    }
}
