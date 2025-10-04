package com.clopez021.mine_arena.core.items;

import com.clopez021.mine_arena.client.ui.WandScreens;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class WandItem extends Item {
  public WandItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (level.isClientSide) {
      DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WandScreens.openWandScreen());
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }
}
