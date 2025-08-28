package com.clopez021.mine_arena.entity;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.spell.SpellEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MineArena.MOD_ID);

	public static final RegistryObject<EntityType<SpellEntity>> SPELL_ENTITY = ENTITIES.register(
			"spell_entity",
			() -> EntityType.Builder.<SpellEntity>of(SpellEntity::new, MobCategory.MISC)
					.sized(1.0f, 1.0f)
					.clientTrackingRange(128)
					.build("spell_entity")
	);

	public static void register(IEventBus eventBus) {
		ENTITIES.register(eventBus);
	}
} 