package com.clopez021.mine_arena.command;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.clopez021.mine_arena.api.Message;
import com.clopez021.mine_arena.api.openrouter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ChatCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        literal("chat")
            .then(argument("message", greedyString()).executes(ChatCommand::sendMessage))
            .then(
                literal("as")
                    .then(
                        argument("role", string())
                            .then(
                                argument("message", greedyString())
                                    .executes(ChatCommand::sendMessage)))));
  }

  protected static int sendMessage(CommandContext<CommandSourceStack> command) {
    String role = "user";
    String message;
    try {
      try {
        role = command.getArgument("role", String.class);
      } catch (Exception ignore) {
      }
      message = command.getArgument("message", String.class);
    } catch (Exception e) {
      command.getSource().sendFailure(Component.literal("Error: missing message text."));
      return 0;
    }

    command
        .getSource()
        .sendSystemMessage(
            Component.literal("Sending to OpenRouter (" + role + "): '" + message + "'"));

    final String finalRole = role;
    CompletableFuture.runAsync(
        () -> {
          try {
            String reply = openrouter.chat(List.of(new Message(finalRole, message)));
            command.getSource().sendSystemMessage(Component.literal("AI: " + reply));
          } catch (Exception e) {
            command.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
          }
        });

    return 1;
  }
}
