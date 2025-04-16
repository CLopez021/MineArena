package com.knkevin.ai_builder.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class ModelHammerItem extends Item {
    public ModelHammerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockPos = pContext.getClickedPos();

        if (!level.isClientSide()) {
            level.setBlockAndUpdate(blockPos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        }

        return InteractionResult.SUCCESS;
    }
}
