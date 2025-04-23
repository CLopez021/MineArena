package com.knkevin.ai_builder.models.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.knkevin.ai_builder.models.ObjModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector4i;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static com.knkevin.ai_builder.AIBuilder.MOD_ID;

/**
 * A class for creating a quantized palette and writing it to a file.
 */
public class Palette {
	/**
	 * How much to increment the quantized colors by.
	 */
	private static final int base = 8;

	/**
	 * A Map of colors as Integers mapped to block names.
	 */
	private static Map<Integer, String> palette = new HashMap<>();

	/**
	 * The text file to write and read the palette as text.
	 */
	public static final String fileName = "palette.json";

	/**
	 * @param color The color to match.
	 * @return The nearest block to that color using the quantized palette.
	 */
	public static BlockState getNearestBlock(int color) {
		int red = Math.min(255 - 255 % base, ((color >> 16 & 0xFF) + base/2) / base * base);
		int green = Math.min(255 - 255 % base, ((color >> 8 & 0xFF) + base/2) / base * base);
		int blue = Math.min(255 - 255 % base, ((color & 0xFF) + base/2) / base * base);
		if ((color >> 24 & 0xFF) != 255) return Blocks.AIR.defaultBlockState();
		int newColor = (255 << 24) | (red << 16) | (green << 8) | blue;
		String blockName = palette.getOrDefault(newColor, ObjModel.DEFAULT_MATERIAL);
		ResourceLocation id = ResourceLocation.tryParse(blockName);
		Block block = ForgeRegistries.BLOCKS.getValue(id);
		if (block == null) return Blocks.STONE.defaultBlockState();
		return block.defaultBlockState();
	}

	public static void loadPaletteFromJSON() {
		try (InputStream inputStream = Palette.class.getResourceAsStream("/data/" + MOD_ID + "/" + fileName)) {
			if (inputStream != null) {
				Map<String, Integer> blockColors = new HashMap<>();
				InputStreamReader reader = new InputStreamReader(inputStream);
				JsonElement json = JsonParser.parseReader(reader);
				for (Map.Entry<String, JsonElement> entry: json.getAsJsonObject().entrySet()) {
					String block = entry.getKey();
					int color = entry.getValue().getAsInt();
					blockColors.put(block, color);
				}
				palette = createQuantizedPalette(blockColors);
			} else {
				System.err.println("Palette JSON file not found");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param blockColors A Map of block names to their average colors.
	 * @return A Map of quantized colors mapped to their corresponding nearest colored blocks.
	 */
	private static Map<Integer, String> createQuantizedPalette(Map<String, Integer> blockColors) {
		Map<Integer, String> quantizedPalette = new HashMap<>();
		for (int r = 0; r <= 256; r += base) {
			for (int g = 0; g <= 256; g += base) {
				for (int b = 0; b <= 256; b += base) {
					Vector4i ARGB = new Vector4i(255, r, g, b);
					String nearestBlock = nearestBlock(blockColors, ARGB);
					quantizedPalette.put(VectorColors.ARGBToInt(ARGB), nearestBlock);
				}
			}
		} return quantizedPalette;
	}

	/**
	 * @param blockColors A Map of block names to their average colors.
	 * @param color The color to match.
	 * @return The name of the block with the closest color to the given color.
	 */
	private static String nearestBlock(Map<String, Integer> blockColors, Vector4i color) {
		String nearestBlock = ObjModel.DEFAULT_MATERIAL;
		double nearestDistance = Double.MAX_VALUE, currentDistance;
		for (Map.Entry<String, Integer> entry: blockColors.entrySet()) {
			Vector4i color2 = VectorColors.intToARGB(entry.getValue());
			currentDistance = VectorColors.colorSquaredDistance(color, color2);
			if (currentDistance > nearestDistance) continue;
			nearestDistance = currentDistance;
			nearestBlock = entry.getKey();
		} return nearestBlock;
	}

}
