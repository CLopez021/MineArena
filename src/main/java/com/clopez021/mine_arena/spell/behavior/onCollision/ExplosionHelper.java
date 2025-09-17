package com.clopez021.mine_arena.spell.behavior.onCollision;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
   * @param ownerId UUID of the owning player for damage attribution and self-protection
   * @param affectOwner Whether to affect the owner player.
   */
  public static void explode(
      Level level, Vec3 center, float radius, float baseDamage, UUID ownerId, boolean affectOwner) {
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

    // Destroy blocks within radius (skip air and unbreakable)
    int minX = Mth.floor(center.x - r);
    int maxX = Mth.floor(center.x + r);
    int minY = Mth.floor(center.y - r);
    int maxY = Mth.floor(center.y + r);
    int minZ = Mth.floor(center.z - r);
    int maxZ = Mth.floor(center.z + r);
    double r2 = r * r;

    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
          double dx = (x + 0.5) - center.x;
          double dy = (y + 0.5) - center.y;
          double dz = (z + 0.5) - center.z;
          double dist2 = dx * dx + dy * dy + dz * dz;
          if (dist2 > r2) continue;

          BlockPos pos = new BlockPos(x, y, z);
          BlockState state = serverLevel.getBlockState(pos);
          if (state.isAir()) continue;
          // Treat blocks with negative destroy speed (e.g., bedrock) as unbreakable
          if (state.getDestroySpeed(serverLevel, pos) < 0) continue;

          serverLevel.destroyBlock(pos, true);
        }
      }
    }

    // Owner lookup (null-safe)
    ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);

    // Apply entity damage and knockback
    AABB box =
        new AABB(
            center.x - r, center.y - r, center.z - r, center.x + r, center.y + r, center.z + r);

    List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box);
    for (LivingEntity entity : targets) {
      if (!affectOwner && entity.getUUID().equals(ownerId)) continue; // skip caster when requested
      double dist = entity.position().distanceTo(center);
      if (dist > r) continue;
      float falloff = 1.0f - (float) (dist / r);

      // Damage with falloff
      float dmg = damage * Math.max(0f, falloff);
      if (dmg > 0f) {
        entity.hurt(serverLevel.damageSources().playerAttack(owner), dmg);
      }

      // Knockback with falloff (scales with radius a bit)
      float baseKb = Math.max(0.4f, r * 0.25f);
      float kb = baseKb * Math.max(0f, falloff);
      if (kb > 0f) {
        double dirX = center.x - entity.getX();
        double dirZ = center.z - entity.getZ();
        entity.knockback(kb, dirX, dirZ);
        // Small upward nudge to feel explosive
        entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.05 * kb, 0.0));
      }
    }
  }
}
