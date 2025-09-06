package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.spell.behavior.onCollision.OnCollisionBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class SpellEntity extends Entity {
    // ---- Synced key (entire config) ----
    private static final EntityDataAccessor<CompoundTag> DATA_CONFIG =
        SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);

    // Local caches (handy for math/render)
    public final Vector3f minCorner = new Vector3f();

	// Authoritative config (single source of truth)
	private SpellEntityConfig config = SpellEntityConfig.empty();

    // Behavior description/handler live in config.getBehavior()
    private boolean collisionTriggered = false;
    private boolean isColliding = false;
	
	// Dynamic dimensions
	private float spanX = 0.5f, spanY = 0.5f, spanZ = 0.5f;
    public float centerLocalX, centerLocalZ;

    public SpellEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        // Spells are actively steered; disable gravity to avoid desync and sudden drops
        this.setNoGravity(true);
    }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(DATA_CONFIG, new CompoundTag());
	}

	// ---------------- Internal apply helpers (no side checks) ----------------

    private void pushConfigToSyncedData() {
        this.entityData.set(DATA_CONFIG, this.config.toNBT());
    }

    // Apply derived runtime state from current config and refresh size
    private void applyDerivedFromConfig() {
        recalcBoundsFromBlocks();
        refreshDimensions();
    }

    // Expose config-backed runtime state without duplicating storage
    public SpellEntityConfig getConfig() { return this.config; }
    public Map<BlockPos, BlockState> getBlocks() { return this.config.getBlocks(); }
    public float getMicroScale() { return this.config.getMicroScale(); }

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
            applyDerivedFromConfig();
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

		// Use SpellEntityConfig's parser and apply as authoritative state
		applyConfigServer(SpellEntityConfig.fromNBT(tag));
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
		
		System.out.println("Recalculated bounds: spanX=" + spanX + ", spanY=" + spanY + ", spanZ=" + spanZ);
		System.out.println("Center: centerLocalX=" + centerLocalX + ", centerLocalZ=" + centerLocalZ);
	}

    /** Apply the given config as the authoritative state (server-side only). */
    private void applyConfigServer(SpellEntityConfig cfg) {
        if (level().isClientSide) return;
        if (cfg == null) cfg = SpellEntityConfig.empty();
        // Apply to synced data and local caches
        this.config = cfg;
        applyDerivedFromConfig();
        pushConfigToSyncedData();
    }

    // ----------------- Collision handling -----------------

    /** Invoke the selected on-collision behavior immediately (server-side). */
    public void triggerCollision() {
        if (!level().isClientSide && !collisionTriggered && this.config.getBehavior().getHandler() != null) {
            collisionTriggered = true;
            this.config.getBehavior().getHandler().accept(this);
        }
    }


    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            // Compute desired motion each tick
            float speed = Math.max(0f, this.config.getMovementSpeed());
            var dir = this.config.getMovementDirection();
            Vec3 v = this.config.getDirectionVector();
            Vec3 motion = Vec3.ZERO;
            if (speed > 0f && dir != null && dir != SpellEntityConfig.MovementDirection.NONE) {
                motion = v.lengthSqr() > 0 ? v.normalize().scale(speed) : Vec3.ZERO;
            }

            this.setDeltaMovement(motion);
            if (!motion.equals(Vec3.ZERO)) this.hasImpulse = true;

            // Move using vanilla pipeline for proper networking/interpolation
            this.move(MoverType.SELF, this.getDeltaMovement());

            // Trigger when we collide; end collision state once no longer colliding.
            boolean collidingNow = this.onGround() || this.horizontalCollision || this.verticalCollision;
            if (collidingNow) {
                if (!collisionTriggered) {
                    triggerCollision();
                }
                isColliding = true;
            } else if (isColliding) {
                // Collision ended this tick
                isColliding = false;
            }
        }
    }

    private Vec3 getOwnerLookOrSelf() {
        try {
            var ownerId = this.config.getOwnerPlayerId();
            if (ownerId != null && this.level() instanceof ServerLevel sl) {
                Player p = sl.getPlayerByUUID(ownerId);
                if (p != null) return p.getLookAngle();
            }
        } catch (Exception ignored) {}
        return this.getLookAngle();
    }

	/**
	 * Initialize from SpellEntityConfig (server-side only).
	 */
	public void initializeServer(SpellEntityConfig data) {
		if (level().isClientSide) {
			throw new IllegalStateException("initializeServer() can only be called on the server side!");
		}

        applyConfigServer(data);
	}
} 
