package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Mutable configuration payload for initializing and updating a SpellEntity. Provides NBT helpers
 * for save/load.
 */
public class SpellEntityConfig extends BaseConfig {
  private Map<BlockPos, BlockState> blocks;
  private float microScale;
  private CollisionBehaviorConfig behavior = new CollisionBehaviorConfig();

  // Movement config
  public enum MovementDirection {
    FORWARD,
    BACKWARD,
    UP,
    DOWN,
    NONE
  }

  private MovementDirection movementDirection = MovementDirection.NONE;
  private float movementSpeed = 0.0f;
  // Cached direction vector once computed; persisted in NBT
  private float dirX = 0f, dirY = 0f, dirZ = 0f;

  // Single canonical constructor
  public SpellEntityConfig(
      Map<BlockPos, BlockState> blocks,
      float microScale,
      CollisionBehaviorConfig behavior,
      MovementDirection direction,
      float speed) {
    this.blocks = blocks != null ? blocks : Map.of();
    this.microScale = microScale;
    this.behavior = behavior != null ? behavior : new CollisionBehaviorConfig();
    this.movementDirection = direction != null ? direction : MovementDirection.NONE;
    this.movementSpeed = speed;

    // Direction vectors are derived lazily via getDirection(playerId)
  }

  public static SpellEntityConfig empty() {
    return new SpellEntityConfig(
        Map.of(), 1.0f, new CollisionBehaviorConfig(), MovementDirection.NONE, 0.0f);
  }

  // Standard getters
  public Map<BlockPos, BlockState> getBlocks() {
    return blocks;
  }

  public float getMicroScale() {
    return microScale;
  }

  public CollisionBehaviorConfig getBehavior() {
    return behavior;
  }

  public MovementDirection getMovementDirection() {
    return movementDirection;
  }

  public float getMovementSpeed() {
    return movementSpeed;
  }

  /**
   * Return a direction vector for this config, computed from player on first use and cached in
   * dirX/Y/Z.
   */
  public Vec3 getDirection(java.util.UUID playerId) {
    // If already cached, return it
    if (dirX != 0f || dirY != 0f || dirZ != 0f) {
      return new Vec3(dirX, dirY, dirZ);
    }

    Vec3 v;
    if (movementDirection == MovementDirection.UP) {
      v = new Vec3(0, 1, 0);
    } else if (movementDirection == MovementDirection.DOWN) {
      v = new Vec3(0, -1, 0);
    } else if (movementDirection == MovementDirection.NONE) {
      v = Vec3.ZERO;
    } else {
      // FORWARD/BACKWARD based on player's current look
      Vec3 look = Vec3.ZERO;
      if (playerId != null) {
        try {
          var server = ServerLifecycleHooks.getCurrentServer();
          if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(playerId);
            if (p != null) look = p.getLookAngle();
          }
        } catch (Exception ignored) {
        }
      }
      v =
          (movementDirection == MovementDirection.BACKWARD)
              ? new Vec3(-look.x, -look.y, -look.z)
              : look;
    }

    // Cache
    this.dirX = (float) v.x;
    this.dirY = (float) v.y;
    this.dirZ = (float) v.z;
    return v;
  }

  // Mutable setters (pydantic-like model)
  public void setBlocks(Map<BlockPos, BlockState> blocks) {
    this.blocks = blocks != null ? blocks : Map.of();
  }

  public void setMicroScale(float microScale) {
    this.microScale = microScale;
  }

  public void setBehavior(CollisionBehaviorConfig behavior) {
    this.behavior = behavior != null ? behavior : new CollisionBehaviorConfig();
  }

  public void setMovementDirection(MovementDirection movementDirection) {
    this.movementDirection = movementDirection != null ? movementDirection : MovementDirection.NONE;
  }

  public void setMovementSpeed(float movementSpeed) {
    this.movementSpeed = movementSpeed;
  }

  // No explicit vector setters; direction derives from movementDirection and player look

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
    tag.putFloat("dirX", dirX);
    tag.putFloat("dirY", dirY);
    tag.putFloat("dirZ", dirZ);
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
    float microScale =
        tag.contains("microScale", Tag.TAG_FLOAT) ? tag.getFloat("microScale") : 1.0f;
    CollisionBehaviorConfig behavior =
        tag.contains("behavior", Tag.TAG_COMPOUND)
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
    float speed =
        tag.contains("movementSpeed", Tag.TAG_FLOAT) ? tag.getFloat("movementSpeed") : 0.0f;

    SpellEntityConfig cfg = new SpellEntityConfig(blocks, microScale, behavior, direction, speed);
    if (tag.contains("dirX", Tag.TAG_FLOAT)) cfg.dirX = tag.getFloat("dirX");
    if (tag.contains("dirY", Tag.TAG_FLOAT)) cfg.dirY = tag.getFloat("dirY");
    if (tag.contains("dirZ", Tag.TAG_FLOAT)) cfg.dirZ = tag.getFloat("dirZ");
    cfg.setBehavior(behavior);
    return cfg;
  }
}
