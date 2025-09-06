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
import java.util.UUID;

/**
 * Mutable configuration payload for initializing and updating a SpellEntity.
 * Provides NBT helpers for save/load.
 */
public class SpellEntityConfig extends BaseConfig {
    private Map<BlockPos, BlockState> blocks;
    private float microScale;
    private CollisionBehaviorConfig behavior = new CollisionBehaviorConfig();
    // Movement config
    public enum MovementDirection { FORWARD, BACKWARD, UP, DOWN, NONE }
    private MovementDirection movementDirection = MovementDirection.NONE;
    private float movementSpeed = 0.0f;
    private UUID ownerPlayerId; // player this spell corresponds to

    // Single canonical constructor
    public SpellEntityConfig(
            Map<BlockPos, BlockState> blocks,
            float microScale,
            String onCollisionKey,
            MovementDirection direction,
            float speed,
            UUID ownerPlayerId
    ) {
        this.blocks = blocks != null ? blocks : Map.of();
        this.microScale = microScale;
        this.behavior.setName(onCollisionKey == null || onCollisionKey.isEmpty() ? "explode" : onCollisionKey);
        this.movementDirection = direction != null ? direction : MovementDirection.NONE;
        this.movementSpeed = speed;
        this.ownerPlayerId = ownerPlayerId;
    }

    public static SpellEntityConfig empty() {
        return new SpellEntityConfig(Map.of(), 1.0f, "explode", MovementDirection.NONE, 0.0f, null);
    }

    // Standard getters
    public Map<BlockPos, BlockState> getBlocks() { return blocks; }
    public float getMicroScale() { return microScale; }
    public CollisionBehaviorConfig getBehavior() { return behavior; }
    public MovementDirection getMovementDirection() { return movementDirection; }
    public float getMovementSpeed() { return movementSpeed; }
    public UUID getOwnerPlayerId() { return ownerPlayerId; }

    // Mutable setters (pydantic-like model)
    public void setBlocks(Map<BlockPos, BlockState> blocks) { this.blocks = blocks != null ? blocks : Map.of(); }
    public void setMicroScale(float microScale) { this.microScale = microScale; }
    public void setBehavior(CollisionBehaviorConfig behavior) { this.behavior = behavior != null ? behavior : new CollisionBehaviorConfig(); }
    public void setMovementDirection(MovementDirection movementDirection) { this.movementDirection = movementDirection != null ? movementDirection : MovementDirection.NONE; }
    public void setMovementSpeed(float movementSpeed) { this.movementSpeed = movementSpeed; }
    public void setOwnerPlayerId(UUID ownerPlayerId) { this.ownerPlayerId = ownerPlayerId; }

    // Note: all other constructors removed to keep a single entry point

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
        tag.putString("movementDirection", movementDirection.name());
        tag.putFloat("movementSpeed", movementSpeed);
        if (ownerPlayerId != null) {
            try {
                tag.putUUID("ownerPlayerId", ownerPlayerId);
            } catch (Throwable t) {
                // Fallback for environments lacking putUUID (shouldn't happen on modern MC)
                tag.putString("ownerPlayerId", ownerPlayerId.toString());
            }
        }
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

        // Movement fields (defaults if absent)
        MovementDirection direction = MovementDirection.NONE;
        if (tag.contains("movementDirection", Tag.TAG_STRING)) {
            try {
                direction = MovementDirection.valueOf(tag.getString("movementDirection"));
            } catch (IllegalArgumentException ignored) {
                direction = MovementDirection.NONE;
            }
        }
        float speed = tag.contains("movementSpeed", Tag.TAG_FLOAT) ? tag.getFloat("movementSpeed") : 0.0f;

        // Owner player UUID if present
        UUID ownerId = null;
        try {
            if (tag.hasUUID("ownerPlayerId")) {
                ownerId = tag.getUUID("ownerPlayerId");
            } else if (tag.contains("ownerPlayerId", Tag.TAG_STRING)) {
                ownerId = UUID.fromString(tag.getString("ownerPlayerId"));
            }
        } catch (Throwable ignored) {}

        SpellEntityConfig cfg = new SpellEntityConfig(blocks, microScale, behavior.getName(), direction, speed, ownerId);
        cfg.setBehavior(behavior);
        return cfg;
    }
}
