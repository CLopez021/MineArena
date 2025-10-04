package com.clopez021.mine_arena.core.entity;

import com.clopez021.mine_arena.spell.SpellCollisionHandler;
import com.clopez021.mine_arena.spell.behavior.collision.SpellEffectBehaviorConfig;
import com.clopez021.mine_arena.spell.config.SpellEntityConfig;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SpellEntity extends Entity {
  // ---- Synced key (entire config) ----
  private static final EntityDataAccessor<CompoundTag> DATA_CONFIG =
      SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);

  // Local caches (handy for math/render)
  public final Vector3f minCorner = new Vector3f();

  // Authoritative config (single source of truth)
  private SpellEntityConfig config = SpellEntityConfig.empty();

  // Behavior description/handler live in config.getEffectBehavior()
  private boolean isColliding = false;
  // Cooldown management to avoid excessive triggering
  private static final int EFFECT_COOLDOWN_TICKS = 20; // 1 second at 20 TPS
  private int ticksSinceLastTrigger = EFFECT_COOLDOWN_TICKS;

  // Dynamic dimensions
  private float spanX = 0.5f, spanY = 0.5f, spanZ = 0.5f;
  public float centerLocalX, centerLocalZ;

  public SpellEntity(EntityType<? extends Entity> type, Level level) {
    super(type, level);
    this.noPhysics = false;
    // Spells are actively steered; disable gravity to avoid desync and sudden drops
    this.setNoGravity(true);
  }

  private UUID ownerPlayerId;

  public void setOwnerPlayerId(UUID id) {
    this.ownerPlayerId = id;
  }

  public UUID getOwnerPlayerId() {
    return this.ownerPlayerId;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    b.define(DATA_CONFIG, new CompoundTag());
  }

  // ---------------- Internal apply helpers (no side checks) ----------------

  private void pushConfigToSyncedData() {
    this.entityData.set(DATA_CONFIG, this.config.toNBT());
  }

  // Expose config-backed runtime state without duplicating storage
  public SpellEntityConfig getConfig() {
    return this.config;
  }

  public Map<BlockPos, BlockState> getBlocks() {
    return this.config.getBlocks();
  }

  public float getMicroScale() {
    return this.config.getMicroScale();
  }

  // ---------------- Server-side setters ----------------

  // Removed granular setters; prefer applying a full config via applyConfigServer

  /** Pack your map -> NBT and set once. Auto-broadcasts to all trackers. */
  // Removed granular setters; prefer applying a full config via applyConfigServer

  // --------------- Client-side: react to updates ---------------

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
    super.onSyncedDataUpdated(key);

    if (key == DATA_CONFIG) {
      CompoundTag cfgTag = this.entityData.get(DATA_CONFIG);
      this.config = SpellEntityConfig.fromNBT(cfgTag);
      recalcBoundsFromBlocks();
    }
  }

  // ----------------- Persistence (server only) -----------------
  @Override
  protected void addAdditionalSaveData(CompoundTag tag) {
    // Delegate to SpellEntityConfig for consistent NBT structure
    tag.merge(this.config.toNBT());
  }

  @Override
  protected void readAdditionalSaveData(CompoundTag tag) {
    if (level().isClientSide) return; // client doesn't load saves
    this.config = SpellEntityConfig.fromNBT(tag);
    pushConfigToSyncedData();
    recalcBoundsFromBlocks();
  }

  // ------------ Dynamic dimensions (canonical approach) ------------

  @Override
  public EntityDimensions getDimensions(Pose pose) {
    // Minecraft's AABB is square in XZ; pick the larger span
    float width = Math.max(spanX, spanZ);
    float height = Math.max(0.1f, spanY);
    return EntityDimensions.fixed(Math.max(0.1f, width), height);
  }

  private void recalcBoundsFromBlocks() {
    Map<BlockPos, BlockState> blocks = this.config.getBlocks();
    float microScale = this.config.getMicroScale();
    if (blocks.isEmpty()) {
      spanX = spanY = spanZ = 0.5f;
      centerLocalX = centerLocalZ = 0f;
      refreshDimensions();
      return;
    }

    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    for (BlockPos p : blocks.keySet()) {
      minX = Math.min(minX, p.getX());
      minY = Math.min(minY, p.getY());
      minZ = Math.min(minZ, p.getZ());
      maxX = Math.max(maxX, p.getX() + 1);
      maxY = Math.max(maxY, p.getY() + 1);
      maxZ = Math.max(maxZ, p.getZ() + 1);
    }

    spanX = (maxX - minX) * microScale;
    spanY = (maxY - minY) * microScale;
    spanZ = (maxZ - minZ) * microScale;

    // local center for rendering alignment
    centerLocalX = ((minX + maxX) * 0.5f) * microScale;
    centerLocalZ = ((minZ + maxZ) * 0.5f) * microScale;

    // keep minCorner cache synced
    this.minCorner.set(minX, minY, minZ);

    // Update entity dimensions based on new bounds
    refreshDimensions();
  }

  // Public: server-only configure
  public void applyConfigServer(SpellEntityConfig config) {
    if (level().isClientSide) return; // client doesn't load saves
    this.config = config != null ? config : SpellEntityConfig.empty();
    pushConfigToSyncedData();
    recalcBoundsFromBlocks();
  }

  // Expose random for collision handler
  public RandomSource getRandomSource() {
    return this.random;
  }

  /**
   * Updates and applies movement based on spell configuration. Computes desired motion from
   * config-provided direction and speed, then moves using vanilla pipeline for proper
   * networking/interpolation.
   */
  private void updateMovement() {
    float speed = Math.max(0f, this.config.getMovementSpeed());
    Vec3 v = this.config.getDirection(ownerPlayerId);
    boolean shouldMove = this.config.getShouldMove();
    Vec3 motion =
        (shouldMove && speed > 0f && v.lengthSqr() > 1e-6) ? v.normalize().scale(speed) : Vec3.ZERO;

    this.setDeltaMovement(motion);
    if (!motion.equals(Vec3.ZERO)) this.hasImpulse = true;

    // Move using vanilla pipeline for proper networking/interpolation
    this.move(MoverType.SELF, this.getDeltaMovement());
  }

  /** Handles on-cast effect triggers that activate immediately without requiring collision. */
  private void handleOnCastTriggers() {
    if (this.config.getEffectBehavior().getTrigger()
        == SpellEffectBehaviorConfig.EffectTrigger.ON_CAST) {
      SpellEffectBehaviorConfig behavior = this.config.getEffectBehavior();
      float radius = Math.max(0.1f, behavior.getRadius());
      boolean affectOwner = behavior.getAffectOwner();
      java.util.List<LivingEntity> affectedEntities =
          SpellCollisionHandler.collectAffectedEntities(this, radius, affectOwner);
      triggerEffect(affectedEntities);
    }
  }

  private void handleCollisionDetection() {
    // Enhanced collision detection: blocks OR entities
    boolean blockCollision = this.onGround() || this.horizontalCollision || this.verticalCollision;

    // Additional entity collision check using AABB overlap
    boolean entityCollision = false;
    if (!blockCollision) {
      List<Entity> nearbyEntities =
          this.level()
              .getEntities(
                  this,
                  this.getBoundingBox(),
                  entity ->
                      entity != this
                          && (this.ownerPlayerId == null
                              || !entity.getUUID().equals(this.ownerPlayerId)));
      entityCollision = !nearbyEntities.isEmpty();
    }

    boolean collidingNow = blockCollision || entityCollision;

    if (collidingNow) {
      // Pre-compute affected entities once for this tick
      var behavior = this.config.getEffectBehavior();
      float radius = Math.max(0.1f, behavior.getRadius());
      boolean affectOwner = behavior.getAffectOwner();
      java.util.List<LivingEntity> affectedEntities =
          SpellCollisionHandler.collectAffectedEntities(this, radius, affectOwner);

      triggerEffect(affectedEntities);
      isColliding = true;
    } else if (isColliding) {
      // Collision ended this tick
      isColliding = false;
    }
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) return;

    // Increment cooldown timer
    if (ticksSinceLastTrigger < EFFECT_COOLDOWN_TICKS) ticksSinceLastTrigger++;

    updateMovement();
    handleOnCastTriggers();
    handleCollisionDetection();
  }

  /** Initialize from SpellEntityConfig (server-side only). */
  public void initializeServer(SpellEntityConfig data) {
    if (level().isClientSide) return;
    applyConfigServer(data);
  }

  public void triggerEffect(java.util.List<LivingEntity> affectedEntities) {
    SpellCollisionHandler.triggerEffect(this, affectedEntities, ticksSinceLastTrigger);
    if (ticksSinceLastTrigger >= EFFECT_COOLDOWN_TICKS) {
      ticksSinceLastTrigger = 0;
    }
  }
}
