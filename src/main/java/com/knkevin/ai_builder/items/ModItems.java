package com.knkevin.ai_builder.items;

import com.knkevin.ai_builder.AIBuilder;
import com.knkevin.ai_builder.items.custom.ModelHammerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AIBuilder.MOD_ID);

    public static final RegistryObject<Item> MODEL_HAMMER = ITEMS.register("model_hammer", () -> new ModelHammerItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
