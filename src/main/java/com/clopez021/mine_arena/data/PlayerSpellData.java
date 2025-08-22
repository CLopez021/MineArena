package com.clopez021.mine_arena.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;
import java.util.*;

/**
 * SavedData for storing player spell and language preferences in world data.
 * This ensures data persists with the world and is properly saved/loaded.
 */
public class PlayerSpellData extends SavedData {
    private static final String DATA_NAME = "mine_arena_player_spells";
    
    // Player data maps
    private final Map<String, List<String>> playerSpells = new HashMap<>();
    private final Map<String, String> playerLanguages = new HashMap<>();
    
    // Default values
    private static final List<String> DEFAULT_SPELLS = List.of();
    private static final String DEFAULT_LANGUAGE = "en-US";
    
    public PlayerSpellData() {
        super();
    }
    /**
     * Gets the global SavedData instance (stored in overworld).
     * This ensures player data persists across all dimensions.
     * 
     * @param server The MinecraftServer to get overworld from
     * @return The PlayerSpellData instance
     */
    public static PlayerSpellData get(net.minecraft.server.MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
            new Factory<PlayerSpellData>(PlayerSpellData::new, PlayerSpellData::load, null),
            DATA_NAME
        );
    }
    
    /**
     * Loads data from NBT.
     * 
     * @param tag The NBT tag to load from
     * @param registries The registry lookup
     * @return A new PlayerSpellData instance
     */
    public static PlayerSpellData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerSpellData data = new PlayerSpellData();
        
        // Load spell data
        if (tag.contains("PlayerSpells", Tag.TAG_COMPOUND)) {
            CompoundTag spellsTag = tag.getCompound("PlayerSpells");
            for (String playerId : spellsTag.getAllKeys()) {
                ListTag spellList = spellsTag.getList(playerId, Tag.TAG_STRING);
                List<String> spells = new ArrayList<>();
                for (Tag spellTag : spellList) {
                    spells.add(spellTag.getAsString());
                }
                data.playerSpells.put(playerId, spells);
            }
        }
        
        // Load language data
        if (tag.contains("PlayerLanguages", Tag.TAG_COMPOUND)) {
            CompoundTag languagesTag = tag.getCompound("PlayerLanguages");
            for (String playerId : languagesTag.getAllKeys()) {
                String language = languagesTag.getString(playerId);
                data.playerLanguages.put(playerId, language);
            }
        }
        
        return data;
    }
    
    /**
     * Saves data to NBT.
     * 
     * @param tag The NBT tag to save to
     * @param registries The registry lookup
     * @return The modified NBT tag
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Save spell data
        CompoundTag spellsTag = new CompoundTag();
        for (Map.Entry<String, List<String>> entry : playerSpells.entrySet()) {
            ListTag spellList = new ListTag();
            for (String spell : entry.getValue()) {
                spellList.add(StringTag.valueOf(spell));
            }
            spellsTag.put(entry.getKey(), spellList);
        }
        tag.put("PlayerSpells", spellsTag);
        
        // Save language data
        CompoundTag languagesTag = new CompoundTag();
        for (Map.Entry<String, String> entry : playerLanguages.entrySet()) {
            languagesTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("PlayerLanguages", languagesTag);
        
        return tag;
    }
    
    /**
     * Gets the spell list for a player.
     * 
     * @param playerId Player UUID as string
     * @return List of spells, or default if none configured
     */
    public List<String> getSpells(String playerId) {
        return new ArrayList<>(playerSpells.getOrDefault(playerId, DEFAULT_SPELLS));
    }
    
    /**
     * Gets the language for a player.
     * 
     * @param playerId Player UUID as string
     * @return Language code, or default if none configured
     */
    public String getLanguage(String playerId) {
        return playerLanguages.getOrDefault(playerId, DEFAULT_LANGUAGE);
    }
    
    /**
     * Sets the spell list for a player.
     * 
     * @param playerId Player UUID as string
     * @param spells List of spells
     */
    public void setSpells(String playerId, List<String> spells) {
        playerSpells.put(playerId, new ArrayList<>(spells));
        setDirty();
    }
    
    /**
     * Sets the language for a player.
     * 
     * @param playerId Player UUID as string
     * @param language Language code
     */
    public void setLanguage(String playerId, String language) {
        playerLanguages.put(playerId, language);
        setDirty();
    }
    

} 