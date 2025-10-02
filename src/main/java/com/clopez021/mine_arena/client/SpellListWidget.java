package com.clopez021.mine_arena.client;

import com.clopez021.mine_arena.packets.SpellInfoListPacket;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;

// Spell list widget
public class SpellListWidget extends ObjectSelectionList<SpellListWidget.Entry> {
  public SpellListWidget(
      Minecraft mc,
      int width,
      int height,
      int y0,
      int y1,
      int itemHeight,
      List<SpellInfoListPacket.SpellInfo> spells) {
    super(mc, width, height, y0, itemHeight);
    updateSpells(spells);
  }

  public void updateSpells(List<SpellInfoListPacket.SpellInfo> spells) {
    if (spells != null) {
      this.clearEntries();
      for (SpellInfoListPacket.SpellInfo spell : spells) {
        this.addEntry(new Entry(spell));
      }
    }
  }

  @Override
  public int getRowWidth() {
    return this.width - 10;
  }

  @Override
  protected int getScrollbarPosition() {
    return this.getX() + this.width - 6;
  }

  public class Entry extends ObjectSelectionList.Entry<Entry> {
    private final SpellInfoListPacket.SpellInfo spell;

    public Entry(SpellInfoListPacket.SpellInfo spell) {
      this.spell = spell;
    }

    @Override
    public Component getNarration() {
      return Component.literal(spell.name + " - " + spell.phrase);
    }

    @Override
    public void render(
        net.minecraft.client.gui.GuiGraphics graphics,
        int index,
        int top,
        int left,
        int width,
        int height,
        int mouseX,
        int mouseY,
        boolean isHovered,
        float partialTick) {
      Font font = Minecraft.getInstance().font;
      graphics.drawString(font, spell.name, left + 2, top + 2, 0xFFFFFF, false);
      graphics.drawString(font, "\"" + spell.phrase + "\"", left + 2, top + 12, 0xAAAAAA, false);
    }
  }
}
