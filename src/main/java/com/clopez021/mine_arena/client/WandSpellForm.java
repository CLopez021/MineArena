package com.clopez021.mine_arena.client;

import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.WandSubmitPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

public class WandSpellForm extends Screen {
    private EditBox spellDescriptionBox;
    private EditBox castPhraseBox;

    public WandSpellForm() {
        super(Component.literal("Wand Spell Form"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxWidth = 200;
        int boxHeight = 20;

        Font font = Minecraft.getInstance().font;

        spellDescriptionBox = new EditBox(font, centerX - boxWidth / 2, centerY - 40, boxWidth, boxHeight, Component.literal("Spell Description"));
        spellDescriptionBox.setMaxLength(256);
        this.addRenderableWidget(spellDescriptionBox);

        // Label above Spell Description
        int labelPadding = 5;
        StringWidget spellDescLabel = new StringWidget(Component.literal("Spell Description:"), font);
        spellDescLabel.setPosition(spellDescriptionBox.getX(), spellDescriptionBox.getY() - font.lineHeight - labelPadding);
        this.addRenderableOnly(spellDescLabel);

        castPhraseBox = new EditBox(font, centerX - boxWidth / 2, centerY - 10, boxWidth, boxHeight, Component.literal("Cast Phrase"));
        castPhraseBox.setMaxLength(256);
        this.addRenderableWidget(castPhraseBox);

        // Label above Cast Phrase
        StringWidget castPhraseLabel = new StringWidget(Component.literal("Cast Phrase:"), font);
        castPhraseLabel.setPosition(castPhraseBox.getX(), castPhraseBox.getY() - font.lineHeight - labelPadding);
        this.addRenderableOnly(castPhraseLabel);

        this.addRenderableWidget(Button.builder(Component.literal("Submit"), (b) -> submit())
                .pos(centerX - 100, centerY + 20)
                .size(90, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), (b) -> onClose())
                .pos(centerX + 10, centerY + 20)
                .size(90, 20)
                .build());

        setInitialFocus(spellDescriptionBox);
    }

    private void submit() {
        String description = spellDescriptionBox.getValue();
        String phrase = castPhraseBox.getValue();
        PacketHandler.INSTANCE.send(new WandSubmitPacket(description, phrase), PacketDistributor.SERVER.noArg());
        // Close screen and start manager-controlled loading with 2-minute timeout
        Minecraft.getInstance().setScreen(null);
        WandScreens.startLoadingWithTimeout(2L * 60L * 1000L);
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
