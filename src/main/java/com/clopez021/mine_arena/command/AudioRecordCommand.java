package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.voicechat.RecorderManager;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class AudioRecordCommand {
    
    /**
     * Starts recording audio for the player who executed the command
     * @param command The executed command context
     * @return 1 for success, 0 for failure
     */
    protected static int startRecording(CommandContext<CommandSourceStack> command) {
        try {
            // Get the player who executed the command
            ServerPlayer player = command.getSource().getPlayerOrException();
            UUID playerUuid = player.getUUID();
            
            // Extract the command argument for context
            String tempCommandArg;
            try {
                tempCommandArg = command.getArgument("command", String.class);
            } catch (Exception e) {
                tempCommandArg = "unknown_command";
            }
            final String commandArg = tempCommandArg;
            
            // Check if player is already being recorded
            if (RecorderManager.isRecording(playerUuid)) {
                command.getSource().sendFailure(Component.literal("Error: You are already being recorded! Please wait for the current 5-second recording to finish."));
                return 0;
            }
            
            // Start recording
            boolean success = RecorderManager.startRecording(playerUuid, commandArg);
            
            if (success) {
                command.getSource().sendSuccess(() -> Component.literal("Started 5-second audio recording for command: " + commandArg), false);
                command.getSource().sendSuccess(() -> Component.literal("Recording will automatically save in 5 seconds..."), false);
                return 1;
            } else {
                command.getSource().sendFailure(Component.literal("Error: Failed to start recording. Make sure Simple Voice Chat is installed and working."));
                return 0;
            }
            
        } catch (Exception e) {
            command.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
