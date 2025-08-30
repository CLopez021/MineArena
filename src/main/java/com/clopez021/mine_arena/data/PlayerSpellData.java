package com.clopez021.mine_arena.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/**
 * SavedData for storing player spell and language preferences in world data.
 * This ensures data persists with the world and is properly saved/loaded.
 */
public class PlayerSpellData extends SavedData {
    private static final String DATA_NAME = "mine_arena_player_spells";
    
    // Player data maps (on-disk: list of PlayerSpell per player)
    // playerId -> List<PlayerSpell>
    private final Map<String, List<PlayerSpell>> playerSpells = new HashMap<>();
    private final Map<String, String> playerLanguages = new HashMap<>();
    
    // Default values
    private static final List<PlayerSpell> DEFAULT_SPELLS = List.of();
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
     * Lifecycle: Called by Minecraft when the SavedData factory resolves this
     * instance via `computeIfAbsent` in `get(...)` below. This happens when the
     * overworld data storage initializes (world load) or the first time this
     * data is accessed in a session. If no prior data exists, the default
     * constructor is used.
     *
     * Format: for each player, a ListTag of CompoundTags with keys
     *   { name, phrase, entityDataFile }.
     *
     * @param tag The NBT tag to load from
     * @param registries The registry lookup
     * @return A new PlayerSpellData instance
     */
    public static PlayerSpellData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerSpellData data = new PlayerSpellData();
        
        // Load spells (current format only)
        if (tag.contains("PlayerSpells", Tag.TAG_COMPOUND)) {
            CompoundTag spellsRoot = tag.getCompound("PlayerSpells");
            for (String playerId : spellsRoot.getAllKeys()) {
                Tag playerTag = spellsRoot.get(playerId);
                List<PlayerSpell> list = new ArrayList<>();
                if (playerTag instanceof ListTag spellList) {
                    for (Tag t : spellList) {
                        if (t instanceof CompoundTag ct) {
                            String name = ct.getString("name");
                            String phrase = ct.getString("phrase");
                            String entityDataFile = ct.getString("entityDataFile");
                            if (isNonEmpty(name) && isNonEmpty(phrase) && isNonEmpty(entityDataFile)) {
                                try {
                                    list.add(new PlayerSpell(name, phrase, entityDataFile));
                                } catch (IllegalArgumentException ignored) { }
                            }
                        }
                    }
                }
                data.playerSpells.put(playerId, list);
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
     * Lifecycle: Called by Minecraft when the world saves if this data has been
     * marked dirty via `setDirty()`. We call `setDirty()` in mutators like
     * `setSpells` and `setLanguage` to ensure persistence.
     *
     * Whatever we output in save is the same one that is read in load.
     * Format:
     * - Writes the new compact format: for each player, a CompoundTag mapping
     *   `phrase -> file`.
     *
     * @param tag The NBT tag to save to
     * @param registries The registry lookup
     * @return The modified NBT tag
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Save spell data as list of { name, phrase, entityDataFile } per player
        CompoundTag spellsRoot = new CompoundTag();
        for (Map.Entry<String, List<PlayerSpell>> entry : playerSpells.entrySet()) {
            ListTag listTag = new ListTag();
            for (PlayerSpell ps : entry.getValue()) {
                CompoundTag ct = new CompoundTag();
                ct.putString("name", ps.name());
                ct.putString("phrase", ps.phrase());
                ct.putString("entityDataFile", ps.entityDataFile());
                listTag.add(ct);
            }
            spellsRoot.put(entry.getKey(), listTag);
        }
        tag.put("PlayerSpells", spellsRoot);
        
        // Save language data
        CompoundTag languagesTag = new CompoundTag();
        for (Map.Entry<String, String> entry : playerLanguages.entrySet()) {
            languagesTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("PlayerLanguages", languagesTag);
        
        return tag;
    }
    
    /**
     * Gets the spells for a player keyed by phrase.
     *
     * @param playerId Player UUID as string
     * @return Map of phrase -> PlayerSpell, or empty map if none configured
     */
    public Map<String, PlayerSpell> getSpells(String playerId) {
        List<PlayerSpell> list = playerSpells.getOrDefault(playerId, DEFAULT_SPELLS);
        Map<String, PlayerSpell> map = new HashMap<>();
        for (PlayerSpell ps : list) {
            map.put(ps.phrase(), ps);
        }
        return map;
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
     * Sets the spells for a player keyed by phrase.
     *
     * @param playerId Player UUID as string
     * @param spells Map of phrase -> PlayerSpell
     */
    public void setSpells(String playerId, Map<String, PlayerSpell> spells) {
        playerSpells.put(playerId, new ArrayList<>(spells.values()));
        setDirty();
    }

    private static boolean isNonEmpty(String s) {
        return s != null && !s.isBlank();
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
