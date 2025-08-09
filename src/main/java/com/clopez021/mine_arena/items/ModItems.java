package com.clopez021.mine_arena.items;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.items.custom.ModelHammerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MineArena.MOD_ID);

    public static final RegistryObject<Item> MODEL_HAMMER = ITEMS.register("model_hammer", () -> new ModelHammerItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
