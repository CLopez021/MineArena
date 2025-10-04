package com.clopez021.mine_arena.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

public final class TagDump {
  private TagDump() {}

  // TODO: Use these to dump all tags to context
  public static void dumpAllBlockTags(RegistryAccess access, Logger log) {
    if (access == null || log == null) return;
    Registry<Block> reg = access.registryOrThrow(Registries.BLOCK);
    reg.getTagNames()
        .forEach(
            (TagKey<Block> key) -> {
              var setOpt = reg.getTag(key);
              int size = setOpt.map(HolderSet::size).orElse(0);
              log.info("#{} ({} entries)", key.location(), size);
              setOpt.ifPresent(
                  set ->
                      set.stream()
                          .map(Holder::value)
                          .forEach(
                              b -> {
                                ResourceLocation id = reg.getKey(b);
                                if (id != null) log.info("  - {}", id);
                              }));
            });
  }

  public static void dumpAllEntityTypeTags(RegistryAccess access, Logger log) {
    if (access == null || log == null) return;
    Registry<EntityType<?>> reg = access.registryOrThrow(Registries.ENTITY_TYPE);
    reg.getTagNames()
        .forEach(
            (TagKey<EntityType<?>> key) -> {
              var setOpt = reg.getTag(key);
              int size = setOpt.map(HolderSet::size).orElse(0);
              log.info("#{} ({} entries)", key.location(), size);
              setOpt.ifPresent(
                  set ->
                      set.stream()
                          .map(Holder::value)
                          .forEach(
                              t -> {
                                ResourceLocation id = reg.getKey(t);
                                if (id != null) log.info("  - {}", id);
                              }));
            });
  }
}
