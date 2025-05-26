package com.knkevin.ai_builder.command;

import com.knkevin.ai_builder.api.MeshyExceptions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

import static com.knkevin.ai_builder.api.Meshy.textTo3D;

public class AICommand {
    public static boolean isGenerating = false;
    public static boolean isCancelling = false;

    /**
     * Generates a 3D model from the given prompt.
     * @param command The executed command.
     * @return A 1 or 0 representing the success of the command.
     */
    protected static int generateModel(CommandContext<CommandSourceStack> command) {
        CompletableFuture.runAsync(() -> {
            try {
                if (isGenerating || isCancelling) {
                    command.getSource().sendFailure(Component.literal("You can only generate one model at a time! Cancel the current generation with /model ai cancel."));
                    return;
                }
                isGenerating = true;
                textTo3D(command);
            } catch (Exception e) {
                command.getSource().sendFailure(Component.literal(e.getLocalizedMessage()));
            } finally {
                isGenerating = false;
                isCancelling = false;
            }
        });
        return 1;
    }

    /**
     * Cancels the current model generation
     * @param command The executed command.
     * @return A 1 or 0 representing the success of the command.
     */
    protected static int cancelGeneration(CommandContext<CommandSourceStack> command) {
        if (!isGenerating) {
            command.getSource().sendSystemMessage(Component.literal("There are no current generations in progress."));
            return 1;
        }
        if (isCancelling) {
            command.getSource().sendFailure(Component.literal("Currently cancelling the generation."));
            return 0;
        }
        isCancelling = true;
        return 1;
    }
}
