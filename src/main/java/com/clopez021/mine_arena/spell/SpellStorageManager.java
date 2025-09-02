package com.clopez021.mine_arena.spell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Stores and retrieves spell init data on a per-player basis using the
 * player's persistent NBT (no world SavedData or JSON files on disk).
 *
 * Keys under player persistent tag "mine_arena":
 * - SpellModels: Compound where each key is a sanitized spell name and value is a JSON string of the model
 *
 * Returns the provided key for lookups; PlayerSpell embeds data directly now.
 */
public class SpellStorageManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    // No sanitization required; key is used as-is and stored per-player
    private static String keyOf(String name) { return name == null ? "" : name; }

    private static CompoundTag getRoot(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound("mine_arena");
        persistent.put("mine_arena", root);
        return root;
    }

    private static CompoundTag getModelsCompound(ServerPlayer player) {
        CompoundTag root = getRoot(player);
        CompoundTag models = root.getCompound("SpellModels");
        root.put("SpellModels", models);
        return models;
    }

    /**
     * Saves a model under the player's persistent data and returns the key to reference it.
     */
    public static String saveSpell(ServerPlayer player, String spellName, SpellEntityInitData initData) {
        try {
            String key = keyOf(spellName);
            if (key.isEmpty()) return null;
            JsonObject model = new JsonObject();
            model.add("model", initData.toJson());
            String json = GSON.toJson(model);
            CompoundTag models = getModelsCompound(player);
            models.putString(key, json);
            // write back
            CompoundTag root = getRoot(player);
            root.put("SpellModels", models);
            player.getPersistentData().put("mine_arena", root);
            return key;
        } catch (Exception e) {
            LOGGER.error("Failed to save spell '{}' for {}", spellName, player.getGameProfile().getName(), e);
            return null;
        }
    }

    /**
     * Loads a model from the player's persistent data via the given key.
     */
    public static SpellEntityInitData loadSpell(ServerPlayer player, String key) {
        try {
            if (key == null || key.isBlank()) return null;
            CompoundTag models = getModelsCompound(player);
            if (!models.contains(key, Tag.TAG_STRING)) return null;
            String json = models.getString(key);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("model")) return null;
            return SpellEntityInitData.fromJson(obj.getAsJsonObject("model"));
        } catch (Exception e) {
            LOGGER.error("Failed to load spell '{}' for {}", key, player.getGameProfile().getName(), e);
            return null;
        }
    }

    public static boolean spellExists(ServerPlayer player, String key) {
        if (key == null || key.isBlank()) return false;
        CompoundTag models = getModelsCompound(player);
        return models.contains(key, Tag.TAG_STRING);
    }

    public static boolean deleteSpell(ServerPlayer player, String key) {
        if (key == null || key.isBlank()) return false;
        CompoundTag models = getModelsCompound(player);
        if (models.contains(key, Tag.TAG_STRING)) {
            models.remove(key);
            CompoundTag root = getRoot(player);
            root.put("SpellModels", models);
            player.getPersistentData().put("mine_arena", root);
        }
        return true;
    }
}
