package com.clopez021.mine_arena.entity;

import com.clopez021.mine_arena.MineArena;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MineArena.MOD_ID);

	public static final RegistryObject<EntityType<ModelEntity>> MODEL_ENTITY = ENTITIES.register(
			"model_entity",
			() -> EntityType.Builder.<ModelEntity>of(ModelEntity::new, MobCategory.MISC)
					.sized(1.0f, 1.0f)
					.clientTrackingRange(128)
					.build("model_entity")
	);

	public static void register(IEventBus eventBus) {
		ENTITIES.register(eventBus);
	}
} 