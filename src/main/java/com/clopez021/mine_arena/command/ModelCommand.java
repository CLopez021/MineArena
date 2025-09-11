package com.clopez021.mine_arena.command;

import com.clopez021.mine_arena.MineArena;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import com.clopez021.mine_arena.models.Model;
import java.util.HashMap;
/**
 * The main class for the "model" command.
 */
public class ModelCommand {
	/**
	 * Registers the "model" command and all of its subcommands.
	 * @param dispatcher CommandDispatcher to register commands.
	 */
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("model")
			.then(literal("place").executes(PlaceCommand::place))
            .then(literal("generate")
                .then(literal("cancel").executes(GenerateCommand::cancelGeneration))
                // Use string() for prompt so additional args can follow
                .then(argument("prompt", string()).executes(GenerateCommand::generateModel)
                    // Prefer parsing save before model_name so "true/false" isn't captured as name
                    .then(argument("save", bool()).executes(GenerateCommand::generateModel))
                    // Optional model name
                    .then(argument("model_name", string()).executes(GenerateCommand::generateModel)
                        // Optional save flag after model name
                        .then(argument("save", bool()).executes(GenerateCommand::generateModel))
                    )
                )
            )
            );
	}

	public static Map<BlockPos, BlockState> buildVoxels(Model model) {
		if (model == null) return Map.of();
		Map<BlockPos, BlockState> blocks = model.getTextureToBlocks();
		return new HashMap<>(blocks);
	}

	/**
	 * Runs when a command tried to execute without a loaded Model.
	 * @param command The executed command.
	 * @return 0
	 */
	protected static int noModelLoaded(CommandContext<CommandSourceStack> command) {
		command.getSource().sendFailure(Component.literal("Error: Load a Model first."));
		return 0;
	}

    

}
