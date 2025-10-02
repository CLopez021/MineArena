package com.clopez021.mine_arena.client;

import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.SpellInfoListPacket;
import com.clopez021.mine_arena.packets.WandSubmitPacket;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

public class WandSpellForm extends Screen {
  // Layout constants
  private static final int INPUT_BOX_WIDTH = 200;
  private static final int INPUT_BOX_HEIGHT = 20;
  private static final int TOP_PADDING = 30;
  private static final int SECTION_PADDING = 20;
  private static final int LABEL_PADDING = 5;
  private static final int BUTTON_WIDTH = 90;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BUTTON_SPACING = 10;
  private static final int LIST_MAX_WIDTH = 400;
  private static final int LIST_HORIZONTAL_MARGIN = 40;
  private static final int LIST_BOTTOM_MARGIN = 30;
  private static final int LIST_ITEM_HEIGHT = 25;

  private EditBox spellDescriptionBox;
  private EditBox castPhraseBox;
  private SpellListWidget spellList;
  private List<SpellInfoListPacket.SpellInfo> spells;

  public WandSpellForm(List<SpellInfoListPacket.SpellInfo> spells) {
    super(Component.literal("Wand Spell Form"));
    this.spells = spells != null ? spells : new java.util.ArrayList<>();
  }

  @Override
  protected void init() {
    int centerX = this.width / 2;
    int centerY = this.height / 2;

    Font font = Minecraft.getInstance().font;

    spellDescriptionBox =
        new EditBox(
            font,
            centerX - INPUT_BOX_WIDTH / 2,
            centerY - 40 - TOP_PADDING,
            INPUT_BOX_WIDTH,
            INPUT_BOX_HEIGHT,
            Component.literal("Spell Description"));
    spellDescriptionBox.setMaxLength(256);
    this.addRenderableWidget(spellDescriptionBox);

    // Label above Spell Description
    StringWidget spellDescLabel = new StringWidget(Component.literal("Spell Description:"), font);
    spellDescLabel.setPosition(
        spellDescriptionBox.getX(), spellDescriptionBox.getY() - font.lineHeight - LABEL_PADDING);
    this.addRenderableOnly(spellDescLabel);

    castPhraseBox =
        new EditBox(
            font,
            centerX - INPUT_BOX_WIDTH / 2,
            centerY - 10 - TOP_PADDING + SECTION_PADDING,
            INPUT_BOX_WIDTH,
            INPUT_BOX_HEIGHT,
            Component.literal("Cast Phrase"));
    castPhraseBox.setMaxLength(256);
    this.addRenderableWidget(castPhraseBox);

    // Label above Cast Phrase
    StringWidget castPhraseLabel = new StringWidget(Component.literal("Cast Phrase:"), font);
    castPhraseLabel.setPosition(
        castPhraseBox.getX(), castPhraseBox.getY() - font.lineHeight - LABEL_PADDING);
    this.addRenderableOnly(castPhraseLabel);

    int buttonY = centerY + 20 - TOP_PADDING + SECTION_PADDING;
    this.addRenderableWidget(
        Button.builder(Component.literal("Submit"), (b) -> submit())
            .pos(centerX - BUTTON_WIDTH - BUTTON_SPACING / 2, buttonY)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

    this.addRenderableWidget(
        Button.builder(Component.literal("Cancel"), (b) -> onClose())
            .pos(centerX + BUTTON_SPACING / 2, buttonY)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

    // Spell list below the buttons
    int listY = centerY + 50 - TOP_PADDING + SECTION_PADDING;
    int listWidth = Math.min(LIST_MAX_WIDTH, this.width - LIST_HORIZONTAL_MARGIN);
    int listHeight = this.height - listY - LIST_BOTTOM_MARGIN;

    // Label for spell list
    StringWidget spellListLabel = new StringWidget(Component.literal("Your Spells:"), font);
    spellListLabel.setPosition(centerX - listWidth / 2, listY);
    this.addRenderableOnly(spellListLabel);

    // Spell list widget
    spellList =
        new SpellListWidget(
            this.minecraft,
            listWidth,
            listHeight,
            listY + font.lineHeight + LABEL_PADDING,
            this.height - LIST_BOTTOM_MARGIN,
            LIST_ITEM_HEIGHT,
            spells);
    spellList.setX(centerX - listWidth / 2);
    this.addRenderableWidget(spellList);

    setInitialFocus(spellDescriptionBox);
  }

  public void updateSpellList(List<SpellInfoListPacket.SpellInfo> newSpells) {
    this.spells = newSpells != null ? newSpells : new java.util.ArrayList<>();

    // Update the spell list widget with new data directly
    if (spellList != null) {
      spellList.updateSpells(this.spells);
    }
  }

  private void submit() {
    String description = spellDescriptionBox.getValue();
    String phrase = castPhraseBox.getValue();
    PacketHandler.INSTANCE.send(
        new WandSubmitPacket(description, phrase), PacketDistributor.SERVER.noArg());
    // Close screen and start manager-controlled loading with 2-minute timeout
    Minecraft.getInstance().setScreen(null);
    // Timeout after 2 minutes
    WandScreens.startLoadingWithTimeout(20L * 60L * 1000L);
    this.onClose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
