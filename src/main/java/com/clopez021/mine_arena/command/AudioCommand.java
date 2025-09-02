package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.spell.PlayerSpell;
import com.clopez021.mine_arena.spell.SpellEntityData;
import com.clopez021.mine_arena.player.PlayerManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
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
                .then(argument("command", string()).executes(AudioRecordCommand::startRecording))
            )
            .then(literal("addSpell")
                .then(argument("name", string())
                    .then(argument("phrase", string())
                        .executes(context -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                String name = context.getArgument("name", String.class);
                                String phrase = context.getArgument("phrase", String.class);

                                // Placeholder entity data for now; will be filled by UI/workflow
                                PlayerSpell ps = new PlayerSpell(name, phrase, SpellEntityData.empty());
                                PlayerManager.getInstance().addSpell(player, ps);

                                context.getSource().sendSuccess(() ->
                                    Component.literal("Added spell: " + name + " (" + phrase + ")"), false);

                                return 1;
                            } catch (CommandSyntaxException e) {
                                context.getSource().sendFailure(Component.literal("Command error: " + e.getMessage()));
                                return 0;
                            } catch (IllegalArgumentException e) {
                                context.getSource().sendFailure(Component.literal("Invalid spell: " + e.getMessage()));
                                return 0;
                            }
                        })
                    )
                )
            )
        );
    }
}
