package com.knkevin.ai_builder.items.custom;

import com.google.common.collect.Multimap;
import com.knkevin.ai_builder.AIBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * The Model Hammer is an item to be used when manipulating and viewing the loaded 3d model.
 * Players can right-click on a block to set the new position of the Model.
 */
public class ModelHammerItem extends Item {
    public ModelHammerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_interaction_range");
        ResourceKey<Attribute> reachKey = ResourceKey.create(Registries.ATTRIBUTE, id);
        Holder<Attribute> reachAttribute = BuiltInRegistries.ATTRIBUTE.getHolderOrThrow(reachKey);

        return ItemAttributeModifiers.builder().add(reachAttribute,
            new AttributeModifier(id, 64, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
            .build();
    }

    /**
     * Run when the player right-clicks on a block.
     * @param context The context for this event.
     * @return SUCCESS if there is a Model loaded, FAIL otherwise.
     */
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        if (AIBuilder.model == null || level.isClientSide) return InteractionResult.FAIL;
        positionModel(context.getClickedPos(), context.getClickedFace());
        return InteractionResult.SUCCESS;
    }

    public static void positionModel(BlockPos blockPos, Direction direction) {
        if (AIBuilder.model == null) return;

        //Calculate the position to be one block off the clicked face, centered in that block position.
        Vec3i normal = direction.getNormal();
        Vector3f pos = new Vector3f(
                blockPos.getX() + normal.getX() + .5f,
                blockPos.getY() + normal.getY() + .5f,
                blockPos.getZ() + normal.getZ() + .5f
        );

        //The size of the Model.
        Vector3f size = new Vector3f(AIBuilder.model.maxCorner).mul(AIBuilder.model.scale);

        //Find the new minimum and maximum corners of the Model's bounding box after it has been rotated.
        Vector3f minCorner = new Vector3f();
        Vector3f maxCorner = new Vector3f();
        for (int mask = 0; mask < 8; ++mask) {
            Vector3f corner = new Vector3f(size.x, size.y, size.z).mul((mask & 1) == 1 ? -1 : 1, (mask & 2) == 2 ? -1 : 1, (mask & 4) == 4 ? -1 : 1).rotate(AIBuilder.model.rotation);
            minCorner.min(corner);
            maxCorner.max(corner);
        }

        //Offset the new position of the model from the clicked face so that it is just touching it.
        switch (direction) {
            case UP -> pos.y -= (int) minCorner.y;
            case DOWN -> pos.y -= (int) maxCorner.y;
            case NORTH -> pos.z -= (int) maxCorner.z;
            case SOUTH -> pos.z -= (int) minCorner.z;
            case WEST -> pos.x -= (int) maxCorner.x;
            case EAST -> pos.x -= (int) minCorner.x;
        }
        AIBuilder.model.position.set(pos.x, pos.y, pos.z);

        Player player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal("Set model position to " + pos.x + ", " + pos.y + ", " + pos.z));
    }
}