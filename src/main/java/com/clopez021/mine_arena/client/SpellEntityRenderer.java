package com.clopez021.mine_arena.client;

import com.clopez021.mine_arena.spell.SpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.logging.Logger;

public class SpellEntityRenderer extends EntityRenderer<SpellEntity> {
	private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;
	public SpellEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blockRenderer = context.getBlockRenderDispatcher();
	}

	@Override
	public void render(SpellEntity entity, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buf, int packedLight) {
		super.render(entity, yaw, partialTicks, pose, buf, packedLight);

		pose.pushPose();
		// center local block grid on the entity position (so local 0,0,0 is centered at entity)
		pose.translate(-0.5, 0.0, -0.5);
		// scale blocks by microScale so each unit cube becomes microScale^3 in world units
		pose.scale(entity.microScale, entity.microScale, entity.microScale);

		// draw each block at its local integer offset
		for (var entry : entity.blocks.entrySet()) {
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
				null
			);
			pose.popPose();
		}

		pose.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(SpellEntity entity) {
		return null; // not used; we render block models
	}
} 