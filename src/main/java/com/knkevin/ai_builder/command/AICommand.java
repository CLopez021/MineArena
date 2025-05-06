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
    /**
     * Generates a 3D model from the given prompt.
     * @param command The executed command.
     * @return A 1 or 0 representing the success of the command.
     */
    protected static int generateModel(CommandContext<CommandSourceStack> command) {
        CompletableFuture.runAsync(() -> {
            try {
                textTo3D(command);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return 1;
    }
}
