package com.clopez021.mine_arena.spell;

import com.clopez021.mine_arena.api.Meshy;
import com.clopez021.mine_arena.command.ModelCommand;
import com.clopez021.mine_arena.models.Model;
import com.clopez021.mine_arena.models.ObjModel;
import java.io.File;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builder that orchestrates Meshy text-to-3D generation, voxelizes the model, and produces a {@link
 * SpellEntityConfig}.
 */
public final class SpellEntityConfigBuilder {
  private SpellEntityConfigBuilder() {}

  /**
   * Generate a model using Meshy from the request's prompt, voxelize it, and construct a
   * SpellEntityConfig. Temporary model files are deleted after use.
   *
   * @param request Minimal request describing generation + movement
   * @param collisionBehavior Optional behavior; if null, defaults are used
   */
  public static SpellEntityConfig build(
      SpellEntityConfigRequest request,
      com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig collisionBehavior)
      throws Exception {
    if (request == null) throw new IllegalArgumentException("request cannot be null");
    if (request.getPrompt() == null || request.getPrompt().isEmpty()) {
      throw new IllegalArgumentException("prompt cannot be empty");
    }

    String previewTaskId = Meshy.createPreviewTask(request.getPrompt());
    // silent progress
    IntConsumer noop = p -> {};
    Meshy.waitForTask(previewTaskId, 0, noop);

    String refineTaskId = Meshy.createRefineTask(previewTaskId);
    Meshy.waitForTask(refineTaskId, 50, noop);

    String[] urls = Meshy.retrieveTextTo3dTask(refineTaskId, noop);
    String objUrl = urls[0];
    String mtlUrl = urls[1];
    String textureUrl = urls[2];

    String modelName = refineTaskId; // default unique name
    String textureName = modelName;

    String objPath = "models/" + modelName + ".obj";
    String mtlPath = "models/" + modelName + ".mtl";
    String texturePath = "models/" + textureName + ".png";

    try {
      java.nio.file.Files.createDirectories(java.nio.file.Paths.get("models"));
      Meshy.downloadFile(objUrl, objPath);
      Meshy.downloadFile(mtlUrl, mtlPath);
      Meshy.downloadFile(textureUrl, texturePath);
      Meshy.renameInFile(mtlPath, "texture_0", textureName);

      Model model = new ObjModel(new File(objPath));
      Map<BlockPos, BlockState> blocks = ModelCommand.buildVoxels(model);

      com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig behavior =
          collisionBehavior != null
              ? collisionBehavior
              : new com.clopez021.mine_arena.spell.behavior.onCollision.CollisionBehaviorConfig();

      return new SpellEntityConfig(
          blocks,
          request.getMicroScale(),
          behavior,
          request.getMovementDirection(),
          request.getMovementSpeed());
    } finally {
      // Delete temp files
      try {
        new File(objPath).delete();
      } catch (Exception ignored) {
      }
      try {
        new File(mtlPath).delete();
      } catch (Exception ignored) {
      }
      try {
        new File(texturePath).delete();
      } catch (Exception ignored) {
      }
    }
  }
}
