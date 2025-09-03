package com.clopez021.mine_arena.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration payload for initializing a SpellEntity.
 * JSON helpers removed; NBT helpers retained for sync/persistence symmetry.
 */
public record SpellEntityConfig(Map<BlockPos, BlockState> blocks, float microScale) {

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag blocksList = new ListTag();
        for (var entry : blocks.entrySet()) {
            CompoundTag b = new CompoundTag();
            b.putInt("x", entry.getKey().getX());
            b.putInt("y", entry.getKey().getY());
            b.putInt("z", entry.getKey().getZ());
            b.putInt("blockId", BuiltInRegistries.BLOCK.getId(entry.getValue().getBlock()));
            blocksList.add(b);
        }
        tag.put("blocks", blocksList);
        tag.putFloat("microScale", microScale);
        return tag;
    }

    public static SpellEntityConfig fromNBT(CompoundTag tag) {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        if (tag.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocksList = tag.getList("blocks", Tag.TAG_COMPOUND);
            for (Tag t : blocksList) {
                if (t instanceof CompoundTag ct) {
                    BlockPos pos = new BlockPos(ct.getInt("x"), ct.getInt("y"), ct.getInt("z"));
                    int blockId = ct.getInt("blockId");
                    Block block = BuiltInRegistries.BLOCK.byId(blockId);
                    blocks.put(pos, block.defaultBlockState());
                }
            }
        }
        float microScale = tag.contains("microScale", Tag.TAG_FLOAT) ? tag.getFloat("microScale") : 1.0f;
        return new SpellEntityConfig(blocks, microScale);
    }
}
