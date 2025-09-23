package com.clopez021.mine_arena.events;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.command.AudioCommand;
import com.clopez021.mine_arena.command.ChatCommand;
import com.clopez021.mine_arena.command.ModelCommand;
import com.clopez021.mine_arena.models.util.Palette;
import java.io.File;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.server.command.ConfigCommand;

public class ModEvents {
  /** Events fired on the Forge bus. */
  @Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
  public static class ModForgeEvents {
    @SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event) {
      ModelCommand.register(event.getDispatcher());
      AudioCommand.register(event.getDispatcher());
      ChatCommand.register(event.getDispatcher());
      ConfigCommand.register(event.getDispatcher());
    }
  }

  /** Events fired on the Mod bus. */
  @Mod.EventBusSubscriber(modid = MineArena.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
  public static class ModModEvents {
    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
      Palette.loadPaletteFromJSON();
      File folder = new File("models");
      if (!folder.exists()) folder.mkdir();
    }
  }
}
