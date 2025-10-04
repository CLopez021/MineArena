package com.clopez021.mine_arena.client.renderer;

import com.clopez021.mine_arena.core.entity.SpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class SpellEntityRenderer extends EntityRenderer<SpellEntity> {
  private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;

  public SpellEntityRenderer(EntityRendererProvider.Context context) {
    super(context);
    this.blockRenderer = context.getBlockRenderDispatcher();
  }

  @Override
  public void render(
      SpellEntity entity,
      float yaw,
      float partialTicks,
      PoseStack pose,
      MultiBufferSource buf,
      int packedLight) {
    super.render(entity, yaw, partialTicks, pose, buf, packedLight);

    pose.pushPose();

    // Center the model on entity origin and scale to micro size
    float micro = entity.getMicroScale();
    pose.translate(
        -entity.centerLocalX, // center the model on entity X
        -(entity.minCorner.y * micro), // minY at the entity feet
        -entity.centerLocalZ // center the model on entity Z
        );
    pose.scale(micro, micro, micro);

    // draw each block at its local integer offset
    for (var entry : entity.getBlocks().entrySet()) {
      var localPos = entry.getKey();
      BlockState state = entry.getValue();
      ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());

      pose.pushPose();
      pose.translate(localPos.getX(), localPos.getY(), localPos.getZ());

      blockRenderer.renderSingleBlock(
          state,
          pose,
          buf,
          LightTexture.FULL_BRIGHT,
          OverlayTexture.NO_OVERLAY,
          net.minecraftforge.client.model.data.ModelData.EMPTY,
          null);
      pose.popPose();
    }

    pose.popPose();
  }

  @Override
  public ResourceLocation getTextureLocation(SpellEntity entity) {
    return null; // not used; we render block models
  }
}
