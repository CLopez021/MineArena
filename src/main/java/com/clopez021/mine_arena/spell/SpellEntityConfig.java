package com.clopez021.mine_arena.spell;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
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
 * Mutable configuration payload for initializing and updating a SpellEntity.
 * Provides NBT helpers for save/load.
 */
public class SpellEntityConfig extends BaseConfig {
    private Map<BlockPos, BlockState> blocks;
    private float microScale;
    private CollisionBehaviorConfig behavior = new CollisionBehaviorConfig();

    public SpellEntityConfig(Map<BlockPos, BlockState> blocks, float microScale, String onCollisionKey) {
        this.blocks = blocks != null ? blocks : Map.of();
        this.microScale = microScale;
        this.behavior.setName(onCollisionKey);
    }

    public SpellEntityConfig(Map<BlockPos, BlockState> blocks, float microScale) {
        this(blocks, microScale, "explode");
    }

    public static SpellEntityConfig empty() { return new SpellEntityConfig(Map.of(), 1.0f, "explode"); }

    // Standard getters
    public Map<BlockPos, BlockState> getBlocks() { return blocks; }
    public float getMicroScale() { return microScale; }
    public CollisionBehaviorConfig getBehavior() { return behavior; }

    // Mutable setters (pydantic-like model)
    public void setBlocks(Map<BlockPos, BlockState> blocks) { this.blocks = blocks != null ? blocks : Map.of(); }
    public void setMicroScale(float microScale) { this.microScale = microScale; }
    public void setBehavior(CollisionBehaviorConfig behavior) { this.behavior = behavior != null ? behavior : new CollisionBehaviorConfig(); }

    @Override
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
        tag.put("behavior", behavior.toNBT());
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
        CollisionBehaviorConfig behavior = tag.contains("behavior", Tag.TAG_COMPOUND)
                ? CollisionBehaviorConfig.fromNBT(tag.getCompound("behavior"))
                : new CollisionBehaviorConfig();
        SpellEntityConfig cfg = new SpellEntityConfig(blocks, microScale, behavior.getName());
        cfg.setBehavior(behavior);
        return cfg;
    }
}
