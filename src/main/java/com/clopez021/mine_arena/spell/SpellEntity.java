package com.clopez021.mine_arena.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class SpellEntity extends Entity {
	// ---- NBT key constants ----
	private static final String NBT_BLOCKS_ROOT = "B";
	private static final String NBT_BLOCK_ENTRY_POS = "p";
	private static final String NBT_BLOCK_ENTRY_BLOCK_ID = "b";
	private static final String NBT_POS_X = "x";
	private static final String NBT_POS_Y = "y";
	private static final String NBT_POS_Z = "z";
	private static final String NBT_MIN_CORNER = "minCorner";
	private static final String NBT_MIN_CORNER_X = "x";
	private static final String NBT_MIN_CORNER_Y = "y";
	private static final String NBT_MIN_CORNER_Z = "z";
	private static final String NBT_BLOCKS_SAVE = "blocks_nbt";
	private static final String NBT_MICRO_SCALE = "microScale";

	// ---- Synced keys ----
	private static final EntityDataAccessor<Float> DATA_MICRO =
		SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<CompoundTag> DATA_MIN_CORNER =
		SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);

	// The block map lives in one CompoundTag
	private static final EntityDataAccessor<CompoundTag> DATA_BLOCKS =
		SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);

	// Local caches (handy for math/render)
	public float microScale = 1f / 16f;
	public final Vector3f minCorner = new Vector3f();
	public final Map<BlockPos, BlockState> blocks = new HashMap<>();

	public SpellEntity(EntityType<? extends Entity> type, Level level) {
		super(type, level);
		this.noPhysics = false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(DATA_MICRO, 1f / 16f);
		b.define(DATA_MIN_CORNER, new CompoundTag());
		b.define(DATA_BLOCKS, new CompoundTag());
	}

	// ---------------- Internal apply helpers (no side checks) ----------------

	private void applyMicroScale(float value) {
		this.microScale = value;
		this.entityData.set(DATA_MICRO, value);
	}

	private void applyMinCorner(Vector3f v) {
		this.minCorner.set(v);
		CompoundTag pos = new CompoundTag();
		pos.putFloat(NBT_MIN_CORNER_X, v.x);
		pos.putFloat(NBT_MIN_CORNER_Y, v.y);
		pos.putFloat(NBT_MIN_CORNER_Z, v.z);
		CompoundTag root = new CompoundTag();
		root.put(NBT_MIN_CORNER, pos);
		this.entityData.set(DATA_MIN_CORNER, root);
	}

	private void applyBlocks(Map<BlockPos, BlockState> map) {
		this.blocks.clear();
		this.blocks.putAll(map);

		CompoundTag root = new CompoundTag();
		ListTag list = new ListTag();
		for (var e : map.entrySet()) {
			CompoundTag t = new CompoundTag();
			CompoundTag pos = new CompoundTag();
			pos.putInt(NBT_POS_X, e.getKey().getX());
			pos.putInt(NBT_POS_Y, e.getKey().getY());
			pos.putInt(NBT_POS_Z, e.getKey().getZ());
			t.put(NBT_BLOCK_ENTRY_POS, pos);
			t.putInt(NBT_BLOCK_ENTRY_BLOCK_ID, BuiltInRegistries.BLOCK.getId(e.getValue().getBlock()));
			list.add(t);
		}
		root.put(NBT_BLOCKS_ROOT, list);
		this.entityData.set(DATA_BLOCKS, root);
	}

	// ---------------- Server-side setters ----------------

	public void setMicroScaleServer(float value) {
		if (!level().isClientSide) {
			applyMicroScale(value);
			refreshDimensions();
		}
	}

	public void setMinCornerServer(Vector3f v) {
		if (!level().isClientSide) {
			applyMinCorner(v);
			refreshDimensions();
		}
	}

	/** Pack your map -> NBT and set once. Auto-broadcasts to all trackers. */
	public void setBlocksServer(Map<BlockPos, BlockState> map) {
		if (!level().isClientSide) {
			applyBlocks(map);
			// If diff streaming/broadcast needed, do it here.
		}
	}

	// --------------- Client-side: react to updates ---------------

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);

		if (key == DATA_MICRO) {
			this.microScale = this.entityData.get(DATA_MICRO);
			refreshDimensions();
		} else if (key == DATA_MIN_CORNER) {
			CompoundTag root = this.entityData.get(DATA_MIN_CORNER);
			if (root != null && root.contains(NBT_MIN_CORNER, Tag.TAG_COMPOUND)) {
				CompoundTag pos = root.getCompound(NBT_MIN_CORNER);
				this.minCorner.set(pos.getFloat(NBT_MIN_CORNER_X), pos.getFloat(NBT_MIN_CORNER_Y), pos.getFloat(NBT_MIN_CORNER_Z));
				refreshDimensions();
			}
		} else if (key == DATA_BLOCKS) {
			Map<BlockPos, BlockState> map = rebuildBlocksFromData();
			this.blocks.clear();
			this.blocks.putAll(map);
		}
	}

	private Map<BlockPos, BlockState> rebuildBlocksFromData() {
		CompoundTag root = this.entityData.get(DATA_BLOCKS);
		Map<BlockPos, BlockState> map = new HashMap<>();
		if (root == null || !root.contains(NBT_BLOCKS_ROOT, Tag.TAG_LIST)) return map;

		ListTag list = root.getList(NBT_BLOCKS_ROOT, Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			CompoundTag posTag = entry.getCompound(NBT_BLOCK_ENTRY_POS);
			BlockPos pos = new BlockPos(posTag.getInt(NBT_POS_X), posTag.getInt(NBT_POS_Y), posTag.getInt(NBT_POS_Z));
			int id = entry.getInt(NBT_BLOCK_ENTRY_BLOCK_ID);
			Block block = BuiltInRegistries.BLOCK.byId(id);
			BlockState st = block.defaultBlockState();
			map.put(pos, st);
		}
		return map;
	}

	// ----------------- Persistence (server only) -----------------

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat(NBT_MICRO_SCALE, this.microScale);
		CompoundTag pos = new CompoundTag();
		pos.putFloat(NBT_MIN_CORNER_X, this.minCorner.x);
		pos.putFloat(NBT_MIN_CORNER_Y, this.minCorner.y);
		pos.putFloat(NBT_MIN_CORNER_Z, this.minCorner.z);
		CompoundTag minRoot = new CompoundTag();
		minRoot.put(NBT_MIN_CORNER, pos);
		tag.put(NBT_MIN_CORNER, minRoot);
		tag.put(NBT_BLOCKS_SAVE, this.entityData.get(DATA_BLOCKS).copy());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (level().isClientSide) return; // client doesn't load saves

		if (tag.contains(NBT_MICRO_SCALE)) {
			applyMicroScale(tag.getFloat(NBT_MICRO_SCALE));
		}

		if (tag.contains(NBT_MIN_CORNER, Tag.TAG_COMPOUND)) {
			CompoundTag minRoot = tag.getCompound(NBT_MIN_CORNER);
			CompoundTag pos = minRoot.getCompound(NBT_MIN_CORNER);
			applyMinCorner(new Vector3f(pos.getFloat(NBT_MIN_CORNER_X), pos.getFloat(NBT_MIN_CORNER_Y), pos.getFloat(NBT_MIN_CORNER_Z)));
		}

		if (tag.contains(NBT_BLOCKS_SAVE, Tag.TAG_COMPOUND)) {
			this.entityData.set(DATA_BLOCKS, tag.getCompound(NBT_BLOCKS_SAVE).copy());
			applyBlocks(rebuildBlocksFromData());
		}
	}

	// ------------ Existing AABB helper ------------
	public void setFromModelBounds(Vector3f min, Vector3f max) {
		this.minCorner.set(min);
		AABB local = new AABB(min.x * microScale, min.y * microScale, min.z * microScale,
				max.x * microScale, max.y * microScale, max.z * microScale);
		this.setBoundingBox(local.move(this.position()));
	}

	// ------------ Convenience initialization method ------------
	/**
	 * Initialize the spell entity with all data at once (server-side only).
	 * This is the recommended way to set up a new SpellEntity after creation.
	 */
	public void initializeServer(Map<BlockPos, BlockState> blocks, Vector3f minCorner, Vector3f maxCorner, float microScale) {
		if (level().isClientSide) {
			throw new IllegalStateException("initializeServer() can only be called on the server side!");
		}
		
		setMicroScaleServer(microScale);
		setMinCornerServer(minCorner);
		setBlocksServer(blocks);
		setFromModelBounds(minCorner, maxCorner);
	}

	/**
	 * Initialize from SpellEntityInitData (server-side only).
	 */
	public void initializeServer(SpellEntityInitData data) {
		initializeServer(data.blocks, data.minCorner, data.maxCorner, data.microScale);
	}
} 