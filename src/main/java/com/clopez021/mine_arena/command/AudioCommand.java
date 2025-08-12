package com.clopez021.mine_arena.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * The main class for the "audio" command.
 */
public class AudioCommand {
    
    /**
     * Registers the "audio" command and all of its subcommands.
     * @param dispatcher CommandDispatcher to register commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("audio")
            .then(literal("record")
                .executes(AudioRecordCommand::startRecording)
            )
            .then(literal("transcribe")
                .executes(AudioRecordCommand::startTranscription)
            )
        );
    }
} 