package com.clopez021.mine_arena.command.arguments;

import com.clopez021.mine_arena.MineArena;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModCommandArguments {
  public static DeferredRegister<ArgumentTypeInfo<?, ?>> argTypeRegistry =
      DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, MineArena.MOD_ID);

  public static final RegistryObject<ArgumentTypeInfo<?, ?>> MODELFILE_ARGUMENT =
      argTypeRegistry.register(
          "model_file_argument",
          () ->
              ArgumentTypeInfos.registerByClass(
                  ModelFileArgument.class,
                  SingletonArgumentInfo.contextFree(ModelFileArgument::new)));

  public static void register(IEventBus eventbus) {
    argTypeRegistry.register(eventbus);
  }
}
