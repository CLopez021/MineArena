package com.clopez021.mine_arena.speech_recognition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.net.URI;

/**
 * Client-side UI for voice recognition browser prompts.
 */
@OnlyIn(Dist.CLIENT)
public class VoiceSidecarUi {
    
    /**
     * Shows a confirmation dialog to the user before opening the voice recognition URL.
     * 
     * @param url The voice recognition URL to open
     */
    public static void promptAndOpen(String url) {
        var mc = Minecraft.getInstance();

        // Create a confirmation dialog
        var screen = new ConfirmScreen(yes -> {
            mc.setScreen(null);
            if (yes) {
                // Use Minecraft's built-in helper to open in the OS browser
                Util.getPlatform().openUri(URI.create(url));
            }
        },
        Component.literal("Enable Voice Recognition"),
        Component.literal("Open a browser window to grant microphone permission for voice commands?"),
        Component.literal("Open Browser"),
        Component.literal("Cancel"));

        mc.setScreen(screen);
    }
} 