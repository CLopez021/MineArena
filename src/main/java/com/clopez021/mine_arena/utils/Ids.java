package com.clopez021.mine_arena.utils;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public final class Ids {
    private Ids() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<Block> resolveBlockStrict(RegistryAccess access, String id) {
        if (id == null || id.isEmpty() || access == null) return Optional.empty();
        System.out.println("resolveBlockStrict: " + id);
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return Optional.empty();
        Registry<Block> registry = access.registryOrThrow(Registries.BLOCK);
        System.out.println("registry: " + registry);
        Block value = registry.get(key);
        System.out.println("value: " + value);
        if (value.toString().equals("Block{minecraft:air}")) {
            return Optional.empty();
        }
        return Optional.ofNullable(value);
    }

    public static Optional<EntityType<?>> resolveEntityTypeStrict(RegistryAccess access, String id) {
        if (id == null || id.isEmpty() || access == null) return Optional.empty();
        System.out.println("resolveEntityTypeStrict: " + id);
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return Optional.empty();
        Registry<EntityType<?>> registry = access.registryOrThrow(Registries.ENTITY_TYPE);
        EntityType<?> value = registry.get(key);
        System.out.println("value: " + value);
        if (value.toString().equals("entity.minecraft.pig") && !id.equals("minecraft:pig")) {
            return Optional.empty();
        }
        return Optional.ofNullable(value);
    }
} 