package com.clopez021.mine_arena.command;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Root command for audio-related utilities (e.g., recording), with spell/entity subcommands removed
 * in favor of voice casting.
 */
public class AudioCommand {

  /** Registers the "audio" command tree. */
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        literal("audio")
            .then(
                literal("record")
                    .then(
                        argument("command", string())
                            .executes(AudioRecordCommand::startRecording))));
  }
}
