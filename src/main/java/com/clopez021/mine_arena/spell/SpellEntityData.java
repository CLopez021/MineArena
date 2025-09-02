package com.clopez021.mine_arena.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain data record for a spell's entity/model init data with CODEC helpers.
 * Uses a compact primitive representation to avoid complex codecs for BlockState.
 */
public record SpellEntityData(List<BlockEntry> blocks, float microScale) {

    public static final Codec<BlockEntry> BLOCK_ENTRY_CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.INT.fieldOf("x").forGetter(BlockEntry::x),
            Codec.INT.fieldOf("y").forGetter(BlockEntry::y),
            Codec.INT.fieldOf("z").forGetter(BlockEntry::z),
            Codec.INT.fieldOf("blockId").forGetter(BlockEntry::blockId)
        ).apply(i, BlockEntry::new)
    );

    public static final Codec<SpellEntityData> CODEC = RecordCodecBuilder.create(i ->
        i.group(
            Codec.list(BLOCK_ENTRY_CODEC).fieldOf("blocks").forGetter(SpellEntityData::blocks),
            Codec.FLOAT.fieldOf("microScale").forGetter(SpellEntityData::microScale)
        ).apply(i, SpellEntityData::new)
    );

    public SpellEntityData {
        if (blocks == null) blocks = List.of();
    }

    public static SpellEntityData empty() { return new SpellEntityData(List.of(), 1.0f); }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (BlockEntry e : blocks) {
            CompoundTag ct = new CompoundTag();
            ct.putInt("x", e.x);
            ct.putInt("y", e.y);
            ct.putInt("z", e.z);
            ct.putInt("blockId", e.blockId);
            list.add(ct);
        }
        tag.put("blocks", list);
        tag.putFloat("microScale", microScale);
        return tag;
    }

    public static SpellEntityData fromNBT(CompoundTag tag) {
        List<BlockEntry> list = new ArrayList<>();
        if (tag.contains("blocks", Tag.TAG_LIST)) {
            ListTag blocksList = tag.getList("blocks", Tag.TAG_COMPOUND);
            for (Tag t : blocksList) {
                if (t instanceof CompoundTag ct) {
                    list.add(new BlockEntry(
                        ct.getInt("x"), ct.getInt("y"), ct.getInt("z"), ct.getInt("blockId")
                    ));
                }
            }
        }
        float scale = tag.contains("microScale", Tag.TAG_FLOAT) ? tag.getFloat("microScale") : 1.0f;
        return new SpellEntityData(list, scale);
    }

    /**
     * Helper: convert to runtime SpellEntityInitData.
     */
    public SpellEntityInitData toInitData() {
        java.util.Map<BlockPos, BlockState> map = new java.util.HashMap<>();
        for (BlockEntry e : blocks) {
            Block b = BuiltInRegistries.BLOCK.byId(e.blockId);
            if (b != null) {
                map.put(new BlockPos(e.x, e.y, e.z), b.defaultBlockState());
            }
        }
        return new SpellEntityInitData(map, microScale);
    }

    /**
     * Helper: build from runtime SpellEntityInitData.
     */
    public static SpellEntityData fromInitData(SpellEntityInitData init) {
        List<BlockEntry> list = new ArrayList<>();
        for (var entry : init.blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            int id = BuiltInRegistries.BLOCK.getId(entry.getValue().getBlock());
            list.add(new BlockEntry(pos.getX(), pos.getY(), pos.getZ(), id));
        }
        return new SpellEntityData(list, init.microScale);
    }

    public static record BlockEntry(int x, int y, int z, int blockId) {}
}

