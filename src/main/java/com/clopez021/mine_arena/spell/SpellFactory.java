package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.command.ModelCommand;
import com.clopez021.mine_arena.entity.ModEntities;
import com.clopez021.mine_arena.models.Model;
import com.clopez021.mine_arena.models.util.Triangle;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.SpellCompletePacket;
import com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig;
import com.clopez021.mine_arena.player.PlayerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellFactory {

    // Step 1: Build blocks from a model (hardcoded prompt for now)
    private static Map<BlockPos, BlockState> buildBlocksForPrompt(String prompt) {
        // For testing: synthesize a simple "fireball" as a magma sphere.
        // We still route through ModelCommand.buildVoxels(Model) to match pipeline expectations.
        Model model = new GeneratedSphereModel(3, Blocks.MAGMA_BLOCK.defaultBlockState());
        return ModelCommand.buildVoxels(model);
    }

    // Step 2: Build collision behavior config (hardcoded for now)
    private static CollisionBehaviorConfig buildCollisionBehaviorConfig() {
        return new CollisionBehaviorConfig("explode", 10f, 5f, true);
    }

    // Step 3: Provide movement/misc params (hardcoded for now)
    private static float microScale() { return .4f; }

    //Slow: 0.2–0.5 blocks/tick (4–10 bps)
    //Medium: 0.8–1.2 blocks/tick (16–24 bps)
    //Fast: 1.5 blocks/tick (~30 bps)
    private static float speed() { return .8f; }
    private static SpellEntityConfig.MovementDirection direction() { return SpellEntityConfig.MovementDirection.FORWARD; }

    /**
     * Build a complete SpellEntityConfig using the three-step pipeline above.
     */
    public static SpellEntityConfig buildConfig(ServerPlayer player, String spellDescription, String castPhrase) {
        // 1) Generate blocks from prompt (test prompt: "a fireball")
        Map<BlockPos, BlockState> blocks = buildBlocksForPrompt("a fireball");

        // 2) Collision behavior
        CollisionBehaviorConfig behavior = buildCollisionBehaviorConfig();

        // 3) Movement and misc
        SpellEntityConfig cfg = new SpellEntityConfig(
            blocks,
            microScale(),
            behavior,
            direction(),
            speed(),
            player != null ? player.getUUID() : null
        );

        // Capture a fixed direction vector at cast time based on enum
        if (player != null) {
            switch (direction()) {
                case UP -> cfg.setMovementDirectionVector(0, 1, 0);
                case DOWN -> cfg.setMovementDirectionVector(0, -1, 0);
                case FORWARD -> {
                    Vec3 look = player.getLookAngle();
                    cfg.setMovementDirectionVector(look);
                }
                case BACKWARD -> {
                    Vec3 look = player.getLookAngle();
                    cfg.setMovementDirectionVector((float)-look.x, (float)-look.y, (float)-look.z);
                }
                default -> cfg.setMovementDirectionVector(0, 0, 0);
            }
        }

        return cfg;
    }

    /**
     * Create and spawn the spell entity immediately, then notify client when done.
     */
    public static void createSpell(ServerPlayer player, String spellDescription, String castPhrase) {
        if (player == null || player.server == null) return;

        // Build config synchronously (all steps are local/test stubs)
        SpellEntityConfig cfg = buildConfig(player, spellDescription, castPhrase);

        // Save a reusable PlayerSpellConfig for this player
        PlayerSpellConfig reusable = new PlayerSpellConfig("fireball", castPhrase != null ? castPhrase : "fireball", cfg);
        PlayerManager.getInstance().addSpell(player, reusable);

        // Spawn entity at player position and align orientation
        player.server.execute(() -> {
            var level = player.level();
            var entityType = ModEntities.SPELL_ENTITY.get();
            SpellEntity e = entityType.create(level);
            if (e != null) {
                Vec3 pos = player.position();
                e.setPos(pos.x, pos.y, pos.z);
                e.setYRot(player.getYRot());
                e.setXRot(player.getXRot());
                e.initializeServer(cfg);
                level.addFreshEntity(e);
            }

            // Notify client that spell creation finished
            if (player.connection != null) {
                PacketHandler.INSTANCE.send(new SpellCompletePacket(), PacketDistributor.PLAYER.with(player));
            }
        });
    }

    // Minimal synthetic Model implementation for testing a voxel sphere
    private static class GeneratedSphereModel extends Model {
        private final int radius;
        private final BlockState blockState;

        GeneratedSphereModel(int radius, BlockState blockState) {
            this.radius = Math.max(1, radius);
            this.blockState = blockState;
        }

        @Override
        public Map<BlockPos, BlockState> getTextureToBlocks() {
            Map<BlockPos, BlockState> map = new HashMap<>();
            int r2 = radius * radius;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        int d2 = x*x + y*y + z*z;
                        if (d2 <= r2) {
                            map.put(new BlockPos(x, y, z), blockState);
                        }
                    }
                }
            }
            return map;
        }

        @Override
        protected void centerModel() { /* no-op for synthetic */ }

        @Override
        public List<Triangle> getTriangles() { return new ArrayList<>(); }

        @Override
        protected void updateBlockFaces() { /* no-op for synthetic */ }
    }
}
