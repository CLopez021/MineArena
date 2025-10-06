package com.clopez021.mine_arena.core.events;

import com.clopez021.mine_arena.MineArena;
import com.clopez021.mine_arena.model3d.util.Palette;
import java.io.File;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModEvents {

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
