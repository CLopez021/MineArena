package com.clopez021.mine_arena.spell;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Unified effect application engine. Accepts a single string id and a duration, and attempts to
 * interpret it as one of: - keyword effects: "ignite", "freeze" - mob effect id (e.g.,
 * "minecraft:slowness")
 */
public final class EffectEngine {
  private EffectEngine() {}

  public static void applyUnifiedEffect(
      ServerLevel level, LivingEntity target, String effectId, int durationTicks) {
    if (level == null || target == null) return;
    String id = effectId == null ? "" : effectId.trim();
    if (id.isEmpty()) return;

    // 1) Keywords -----------------------------------------------------------
    if (id.equalsIgnoreCase("ignite") || id.equalsIgnoreCase("minecraft:ignite")) {
      int ticks = Math.max(1, durationTicks);
      target.setRemainingFireTicks(ticks);
      return;
    }
    if (id.equalsIgnoreCase("freeze") || id.equalsIgnoreCase("minecraft:freeze")) {
      int ticks = Math.max(1, durationTicks);
      target.setTicksFrozen(ticks);
      return;
    }

    // 2) Single mob effect by id -------------------------------------------
    Holder<MobEffect> effect = resolveMobEffect(level.registryAccess(), id);
    if (effect != null) {
      int dur = Math.max(1, durationTicks > 0 ? durationTicks : 200);
      target.addEffect(new MobEffectInstance(effect, dur, 0, false, true, true));
    }
  }

  private static Holder<MobEffect> resolveMobEffect(RegistryAccess access, String id) {
    if (access == null || id == null || id.isBlank()) return null;
    Registry<MobEffect> reg = access.registryOrThrow(Registries.MOB_EFFECT);
    ResourceLocation rl = ResourceLocation.tryParse(id);
    if (rl == null) return null;
    return reg.getHolder(ResourceKey.create(Registries.MOB_EFFECT, rl)).orElse(null);
  }
}
