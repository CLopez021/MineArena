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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
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
	private static final String NBT_BLOCKS_SAVE = "blocks_nbt";
	private static final String NBT_MICRO_SCALE = "microScale";

	// ---- Synced keys ----
	private static final EntityDataAccessor<Float> DATA_MICRO =
		SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.FLOAT);

	// The block map lives in one CompoundTag
	private static final EntityDataAccessor<CompoundTag> DATA_BLOCKS =
		SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.COMPOUND_TAG);

	// Local caches (handy for math/render)
	public float microScale = 1f / 16f;
	public final Vector3f minCorner = new Vector3f();
	public final Map<BlockPos, BlockState> blocks = new HashMap<>();
	
	// Dynamic dimensions
	private float spanX = 0.5f, spanY = 0.5f, spanZ = 0.5f;
	public float centerLocalX, centerLocalZ;

	public SpellEntity(EntityType<? extends Entity> type, Level level) {
		super(type, level);
		this.noPhysics = false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder b) {
		b.define(DATA_MICRO, 1f / 16f);
		b.define(DATA_BLOCKS, new CompoundTag());
	}

	// ---------------- Internal apply helpers (no side checks) ----------------

	private void applyMicroScale(float value) {
		this.microScale = value;
		this.entityData.set(DATA_MICRO, value);
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
			recalcBoundsFromBlocks();
			refreshDimensions();
		} else if (key == DATA_BLOCKS) {
			Map<BlockPos, BlockState> map = rebuildBlocksFromData();
			this.blocks.clear();
			this.blocks.putAll(map);
			recalcBoundsFromBlocks();
			refreshDimensions();
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
		tag.put(NBT_BLOCKS_SAVE, this.entityData.get(DATA_BLOCKS).copy());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (level().isClientSide) return; // client doesn't load saves

		if (tag.contains(NBT_MICRO_SCALE)) {
			applyMicroScale(tag.getFloat(NBT_MICRO_SCALE));
		}

		if (tag.contains(NBT_BLOCKS_SAVE, Tag.TAG_COMPOUND)) {
			this.entityData.set(DATA_BLOCKS, tag.getCompound(NBT_BLOCKS_SAVE).copy());
			applyBlocks(rebuildBlocksFromData());
		}
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

	/**
	 * Initialize from SpellEntityInitData (server-side only).
	 */
	public void initializeServer(SpellEntityInitData data) {
		if (level().isClientSide) {
			throw new IllegalStateException("initializeServer() can only be called on the server side!");
		}
		
		setMicroScaleServer(data.microScale);
		setBlocksServer(data.blocks);
		recalcBoundsFromBlocks();
		refreshDimensions(); // now uses our new spans
	}
} 