package com.clopez021.mine_arena.spell.behavior.onCollision;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Utility for custom, configurable explosions that do not use vanilla block-destroying logic. */
public final class ExplosionHelper {
  private ExplosionHelper() {}

  /**
   * Applies area damage with simple linear falloff, spawns particles and sound.
   *
   * @param level Server level
   * @param center Position of the explosion
   * @param radius Affects range and falloff. Non-negative, clamped to minimum 0.1f when used.
   * @param baseDamage Maximum damage at center. Use 0 to deal no damage.
   * @param ownerId Optional UUID of the owning player for damage attribution and self-protection
   */
  public static void explode(
      Level level, Vec3 center, float radius, float baseDamage, UUID ownerId) {
    if (level == null || level.isClientSide) return;
    if (!(level instanceof ServerLevel serverLevel)) return;

    float r = Math.max(0.1f, radius);
    float damage = Math.max(0f, baseDamage);

    // Visuals
    serverLevel.sendParticles(
        ParticleTypes.EXPLOSION, center.x, center.y + 0.1, center.z, 1, 0.0, 0.0, 0.0, 0.0);
    serverLevel.playSound(
        null,
        center.x,
        center.y,
        center.z,
        SoundEvents.GENERIC_EXPLODE,
        SoundSource.BLOCKS,
        4.0f,
        0.9f + (serverLevel.getRandom().nextFloat() * 0.2f));

    if (damage <= 0f) return;

    ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);

    AABB box =
        new AABB(
            center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);

    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box);
    for (LivingEntity entity : targets) {
      if (entity.getUUID().equals(ownerId)) continue; // don't hurt the caster
      double dist = entity.position().distanceTo(center);
      if (dist > r) continue;
      float falloff = 1.0f - (float) (dist / r);
      float dmg = damage * Math.max(0f, falloff);
      if (dmg <= 0f) continue;

      entity.hurt(serverLevel.damageSources().playerAttack(owner), dmg);
    }
  }
}
