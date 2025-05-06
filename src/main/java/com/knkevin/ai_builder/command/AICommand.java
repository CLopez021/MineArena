package com.knkevin.ai_builder.command;

import com.knkevin.ai_builder.api.MeshyExceptions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.IntConsumer;

import static com.knkevin.ai_builder.api.Meshy.textTo3D;

public class AICommand {
    /**
     * Generates a 3D model from the given prompt.
     * @param command The executed command.
     * @return A 1 or 0 representing the success of the command.
     */
    protected static int generateModel(CommandContext<CommandSourceStack> command) {
        String prompt = StringArgumentType.getString(command, "prompt");
        IntConsumer updateProgress = progress -> {
            ServerPlayer player = command.getSource().getPlayer();
            if (player != null) {
                String bar = "§a" + new String(new char[progress / 2]).replace("\0", "|") + "§8" + new String(new char[50 - progress / 2]).replace("\0", "|") + "§f";
                player.displayClientMessage(Component.literal("Progress: [" + bar + "] " + progress + "%"), true);
            }
        };
        try {
            textTo3D(prompt, updateProgress);
        }
        catch (Exception e) {
            command.getSource().sendFailure(Component.literal(e.getMessage()));
            e.printStackTrace();
            return 0;
        }
        command.getSource().getPlayer().displayClientMessage(Component.literal("Generation Complete"), true);
        Component message = Component.literal("Successfully generated a model for '" + prompt + "'.");
        command.getSource().sendSystemMessage(message);
        return 1;
    }
}
