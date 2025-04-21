package com.knkevin.ai_builder.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static com.knkevin.ai_builder.api.Meshy.textTo3D;

public class AICommand {
    /**
     * Generates a 3D model from the given prompt.
     * @param command The executed command.
     * @return A 1 or 0 representing the success of the command.
     */
    protected static int generateModel(CommandContext<CommandSourceStack> command) {
        String prompt = StringArgumentType.getString(command, "prompt");
        textTo3D(prompt);
        Component message = Component.literal(prompt);
        command.getSource().sendSystemMessage(message);
        return 1;
    }
}
