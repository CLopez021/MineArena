package com.clopez021.mine_arena.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class ModelEntity extends Entity {
	public float microScale = 1f/16f;
	public final Map<BlockPos, net.minecraft.world.level.block.state.BlockState> voxels = new HashMap<>();
	public final Vector3f minCorner = new Vector3f();

	public ModelEntity(EntityType<? extends Entity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.contains("microScale")) this.microScale = tag.getFloat("microScale");
		if (tag.contains("minCornerX")) this.minCorner.set(tag.getFloat("minCornerX"), tag.getFloat("minCornerY"), tag.getFloat("minCornerZ"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("microScale", this.microScale);
		tag.putFloat("minCornerX", this.minCorner.x);
		tag.putFloat("minCornerY", this.minCorner.y);
		tag.putFloat("minCornerZ", this.minCorner.z);
	}

	public void setVoxels(Map<BlockPos, net.minecraft.world.level.block.state.BlockState> map) {
		this.voxels.clear();
		this.voxels.putAll(map);
	}

	public void setFromModelBounds(Vector3f min, Vector3f max) {
		this.minCorner.set(min);
		AABB local = new AABB(min.x * microScale, min.y * microScale, min.z * microScale,
				max.x * microScale, max.y * microScale, max.z * microScale);
		this.setBoundingBox(local.move(this.position()));
	}
} 