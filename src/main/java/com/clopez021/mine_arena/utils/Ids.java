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

    // --- Tag-based strict resolution ---

    public static Optional<List<Block>> resolveBlockTagStrict(RegistryAccess access, String tagName) {
        if (tagName == null || tagName.isEmpty() || access == null) return Optional.empty();

        System.out.println("resolveBlockTagStrict: " + tagName);
        
        String s = tagName.startsWith("#") ? tagName.substring(1) : tagName;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        if (rl == null) return Optional.empty();
        Registry<Block> reg = access.registryOrThrow(Registries.BLOCK);

        // Try direct lookup first
        TagKey<Block> key = TagKey.create(Registries.BLOCK, rl);
        var setOpt = reg.getTag(key);

        // Fallback: scan all tag names to find a matching key
        if (setOpt.isEmpty()) {
            AtomicReference<TagKey<Block>> foundRef = new AtomicReference<>(null);
            reg.getTagNames().forEach(candidate -> {
                if (candidate.location().equals(rl)) {
                    foundRef.set(candidate);
                }
            });
            TagKey<Block> found = foundRef.get();
            if (found != null) {
                setOpt = reg.getTag(found);
            }
        }

        if (setOpt.isEmpty()) {
            LOGGER.warn("Block tag not found: {}", rl);
            TagDump.dumpAllBlockTags(access, LOGGER);
            return Optional.empty();
        }
        List<Block> out = new ArrayList<>();
        setOpt.get().forEach(holder -> out.add(holder.value()));
        return out.isEmpty() ? Optional.empty() : Optional.of(out);
    }

    public static Optional<List<EntityType<?>>> resolveEntityTypeTagStrict(RegistryAccess access, String tagName) {
        if (tagName == null || tagName.isEmpty() || access == null) return Optional.empty();
        System.out.println("resolveEntityTypeTagStrict: " + tagName);
        String s = tagName.startsWith("#") ? tagName.substring(1) : tagName;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        if (rl == null) return Optional.empty();
        Registry<EntityType<?>> reg = access.registryOrThrow(Registries.ENTITY_TYPE);

        // Try direct lookup first
        TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, rl);
        var setOpt = reg.getTag(key);

        // Fallback: scan all tag names to find a matching key
        if (setOpt.isEmpty()) {
            AtomicReference<TagKey<EntityType<?>>> foundRef = new AtomicReference<>(null);
            reg.getTagNames().forEach(candidate -> {
                if (candidate.location().equals(rl)) {
                    foundRef.set(candidate);
                }
            });
            TagKey<EntityType<?>> found = foundRef.get();
            if (found != null) {
                setOpt = reg.getTag(found);
            }
        }

        if (setOpt.isEmpty()) {
            LOGGER.warn("EntityType tag not found: {}", rl);
            TagDump.dumpAllEntityTypeTags(access, LOGGER);
            return Optional.empty();
        }
        List<EntityType<?>> out = new ArrayList<>();
        setOpt.get().forEach(holder -> out.add(holder.value()));
        return out.isEmpty() ? Optional.empty() : Optional.of(out);
    }
} 