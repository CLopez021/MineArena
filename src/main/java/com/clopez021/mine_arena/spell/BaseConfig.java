package com.clopez021.mine_arena.spell;

import net.minecraft.nbt.CompoundTag;

/**
 * Base configuration with NBT round-trip methods.
 * Note: static factory methods cannot be abstract in Java; implement a
 * public static fromNBT(CompoundTag) in concrete subclasses as needed.
 */
public abstract class BaseConfig {
    public abstract CompoundTag toNBT();
}
