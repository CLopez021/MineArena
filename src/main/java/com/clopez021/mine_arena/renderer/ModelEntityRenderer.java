package com.clopez021.mine_arena.renderer;

import com.clopez021.mine_arena.entity.ModelEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
public class ModelEntityRenderer extends EntityRenderer<ModelEntity> {
	private final BlockRenderDispatcher blockRenderer;
    public ModelEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
	}

	@Override
	public void render(ModelEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buf, int packedLight) {
        super.render(entity, yaw, partialTicks, pose, buf, packedLight);

        pose.pushPose();

        // Entity position is already applied; center the first cube on the entity
        // (renderSingleBlock draws a [0..1] cube at the current origin)
        pose.translate(-0.5, 0.0, -0.5);

        var state = Blocks.IRON_BLOCK.defaultBlockState();

        // You can sample lighting per block if you want nicer lighting:
        int light0 = LevelRenderer.getLightColor(entity.level(), entity.blockPosition());
        int light1 = LevelRenderer.getLightColor(entity.level(), entity.blockPosition().above());

        // bottom block
        blockRenderer.renderSingleBlock(state, pose, buf, light0, OverlayTexture.NO_OVERLAY);

        // top block (translate up by exactly 1 block)
        pose.translate(0.0, 1.0, 0.0);
        blockRenderer.renderSingleBlock(state, pose, buf, light1, OverlayTexture.NO_OVERLAY);

        pose.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(ModelEntity entity) {
		return null; // not used; we render block models
	}
} 