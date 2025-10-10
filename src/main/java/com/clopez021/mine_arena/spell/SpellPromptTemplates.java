package com.clopez021.mine_arena.spell;

/** Prompt builder utilities for spell-related LLM prompts. */
public final class SpellPromptTemplates {
  private SpellPromptTemplates() {}

  // ---- Entity spawn ID examples ----
  private static final String SPAWN_ID_EXAMPLES =
      """
      Passive/utility: minecraft:allay, minecraft:axolotl, minecraft:cat, minecraft:chicken, minecraft:cow, minecraft:dolphin, minecraft:fox, minecraft:frog, minecraft:glow_squid, minecraft:goat, minecraft:horse, minecraft:mooshroom, minecraft:mule, minecraft:ocelot, minecraft:panda, minecraft:parrot, minecraft:pig, minecraft:polar_bear, minecraft:rabbit, minecraft:salmon, minecraft:sheep, minecraft:squid, minecraft:tropical_fish, minecraft:turtle, minecraft:villager, minecraft:wandering_trader, minecraft:camel, minecraft:sniffer, minecraft:armadillo.
      Neutral/rideable/guards: minecraft:bee, minecraft:enderman, minecraft:llama, minecraft:trader_llama, minecraft:wolf, minecraft:iron_golem, minecraft:strider, minecraft:piglin.
      Hostile: minecraft:blaze, minecraft:bogged, minecraft:breeze, minecraft:cave_spider, minecraft:creeper, minecraft:drowned, minecraft:endermite, minecraft:evoker, minecraft:ghast, minecraft:guardian, minecraft:hoglin, minecraft:husk, minecraft:magma_cube, minecraft:phantom, minecraft:piglin_brute, minecraft:pillager, minecraft:ravager, minecraft:shulker, minecraft:silverfish, minecraft:skeleton, minecraft:slime, minecraft:spider, minecraft:stray, minecraft:vex, minecraft:vindicator, minecraft:witch, minecraft:wither_skeleton, minecraft:zoglin, minecraft:zombie, minecraft:zombie_villager, minecraft:zombified_piglin, minecraft:warden.
      Boss/special: minecraft:ender_dragon, minecraft:wither.
      Projectiles/misc: minecraft:arrow, minecraft:snowball, minecraft:fireball, minecraft:small_fireball, minecraft:dragon_fireball, minecraft:shulker_bullet, minecraft:egg, minecraft:ender_pearl, minecraft:xp_bottle, minecraft:trident, minecraft:area_effect_cloud, minecraft:lightning_bolt, minecraft:tnt, minecraft:falling_block, minecraft:item, minecraft:item_frame, minecraft:glow_item_frame, minecraft:painting, minecraft:leash_knot, minecraft:marker, minecraft:boat, minecraft:chest_boat, minecraft:minecart, minecraft:chest_minecart, minecraft:furnace_minecart, minecraft:hopper_minecart, minecraft:tnt_minecart.""";

  private static final String BLOCK_ID_EXAMPLES =
      """
minecraft:acacia_log, minecraft:acacia_planks, minecraft:amethyst_block, minecraft:andesite, minecraft:bamboo_block, minecraft:bamboo_planks, minecraft:bedrock, minecraft:birch_log, minecraft:birch_planks, minecraft:blue_ice, minecraft:budding_amethyst
minecraft:cactus, minecraft:campfire, minecraft:cherry_log, minecraft:cherry_planks, minecraft:chorus_flower, minecraft:chorus_plant, minecraft:clay, minecraft:coarse_dirt, minecraft:cobweb, minecraft:crimson_nylium
minecraft:crimson_planks, minecraft:crimson_stem, minecraft:crying_obsidian, minecraft:dark_oak_log, minecraft:dark_oak_planks, minecraft:deepslate, minecraft:diorite, minecraft:dirt, minecraft:end_gateway, minecraft:end_portal, minecraft:end_stone, minecraft:fire
minecraft:glow_lichen, minecraft:granite, minecraft:grass_block, minecraft:gravel, minecraft:honey_block, minecraft:ice, minecraft:jungle_log, minecraft:jungle_planks, minecraft:lava, minecraft:light, minecraft:magma_block, minecraft:mangrove_log, minecraft:mangrove_planks,
minecraft:moss_block, minecraft:mud, minecraft:mycelium, minecraft:netherrack, minecraft:nether_portal, minecraft:oak_log, minecraft:oak_planks, minecraft:obsidian, minecraft:pale_moss_block, minecraft:pale_oak_log, minecraft:pale_oak_planks, minecraft:packed_ice,
minecraft:podzol, minecraft:powder_snow, minecraft:red_sand, minecraft:reinforced_deepslate, minecraft:rooted_dirt, minecraft:sand, minecraft:sculk_vein, minecraft:snow, minecraft:snow_block, minecraft:soul_campfire, minecraft:soul_fire,
minecraft:soul_sand, minecraft:soul_soil, minecraft:spruce_log, minecraft:spruce_planks, minecraft:stone, minecraft:suspicious_gravel, minecraft:suspicious_sand, minecraft:sweet_berry_bush,
minecraft:tnt, minecraft:torch, minecraft:tuff, minecraft:vine, minecraft:warped_nylium,minecraft:warped_planks, minecraft:warped_stem, minecraft:water,minecraft:blue_ice""";

  // ---- Status effect ID examples ----
  private static final String STATUS_EFFECT_EXAMPLES =
      """
      minecraft:absorption, minecraft:bad_omen, minecraft:raid_omen, minecraft:trial_omen, minecraft:bad_luck, minecraft:luck, minecraft:blindness, minecraft:darkness, minecraft:conduit_power, minecraft:dolphins_grace,
      minecraft:fire_resistance, minecraft:glowing, minecraft:haste, minecraft:mining_fatigue, minecraft:health_boost, minecraft:hero_of_the_village, minecraft:hunger, minecraft:infested, minecraft:oozing, minecraft:weaving,
      minecraft:wind_charged, minecraft:instant_damage, minecraft:instant_health, minecraft:saturation, minecraft:invisibility, minecraft:jump_boost, minecraft:levitation, minecraft:nausea, minecraft:night_vision, minecraft:poison, minecraft:wither,
      minecraft:regeneration, minecraft:resistance, minecraft:slow_falling, minecraft:slowness, minecraft:speed, minecraft:strength, minecraft:water_breathing, minecraft:weakness.
      Custom effects: ignite (sets target on fire), freeze (freezes target in place).""";

  /** STEP 1: System prompt for behavior-only configuration. */
  public static String step1BehaviorSystemPrompt() {
    return """
        You are configuring a minecraft spell, this is STEP 1 which determines the behavior of the spell.
        The following is the config for the spell:
        {
          radius: number (>0) // Effect radius in blocks. Determines area of impact for damage, knockback, and status effects.
          damage: number (>=0) // Damage in half-hearts (1 damage = 0.5 hearts, 2 damage = 1 heart). Applied directly via magic damage source.
          despawnOnTrigger: boolean // If true, spell entity disappears after triggering effects (one-time use). If false, persists (useful for structures/barriers/DoT zones).
          spawnEntityId: string // Minecraft entity ID to spawn upon the effect being triggered. Useful for spawning entities that match the spell theme. For example, if the spell is a large dog, summon wolves; if it's a bee swarm spell, spawn bees. Use "" for none. Available Entity IDs: %s
          spawnEntityCount: number (>=0) // Number of entities to spawn at random positions within radius. Ignored if spawnEntityId is "".
          placeBlockId: string // Minecraft block ID to place upon the effect being triggered. Useful for creating structures, barriers, or terrain effects. For example, if the spell is a web, place minecraft:cobweb blocks; if it's an ice spell, place minecraft:ice blocks. Use "" for none. Available Block IDs: %s
          placeBlockCount: number (>=0) // Number of blocks to place at random positions within radius. Ignored if placeBlockId is "".
          statusEffectId: string // Status effect ID to apply. Use "" for none. Examples: %s
          statusDurationSeconds: number (>=0) // Duration the status effect lasts in seconds (converted to ticks internally at 20 TPS).
          statusAmplifier: number (>=0) // Effect level minus 1 (0=I, 1=II, 3=IV). For Strength IV use amplifier=3.
          affectOwner: boolean // If true, spell affects the caster. If false, caster is immune to all effects.
          trigger: "onCast" | "onImpact" // When effects activate: "onCast"=immediately at caster's feet, "onImpact"=when spell collides with block/entity (requires shouldMove=true in STEP 2).
          knockbackAmount: number (>=0) // Knockback strength using vanilla Minecraft knockback (0.5=weak, 1.0=moderate, 2.0=strong). Uses distance-based falloff within radius. Adds slight upward velocity.
          blockDestructionRadius: number (>=0) // Horizontal/vertical radius in blocks for terrain destruction (spherical). 0 = no destruction.
          blockDestructionDepth: number (>=0) // Number of inward layers to destroy (radius shrinks by 1 per layer). Creates crater effect. 0 = no destruction.
        }

        Rules:
        - trigger=onCast means effects occur immediately at the caster (no impact needed).
        - trigger=onImpact means effects occur when the entity collides (requires travel later).
        - Empty spawnEntityId, placeBlockId, or statusEffectId means "none" for that effect.
        - spawnEntityId and placeBlockId can both be set to spawn entities AND place blocks simultaneously.
        - All effects (damage, knockback, status) have 1-second cooldown to prevent spam triggering."""
        .formatted(SPAWN_ID_EXAMPLES, BLOCK_ID_EXAMPLES, STATUS_EFFECT_EXAMPLES);
  }

  /** STEP 1: Notepad prompt for behavior reasoning. */
  public static String step1BehaviorNotepadPrompt(String playerIntent) {
    return """
        STEP 1 Notepad, use this to think and plan the spell and the behavior of the spell given the player intent:
        - Player intent: "%s"

        Give justifications for your choices.
        """
        .formatted(playerIntent);
  }

  /** STEP 2: System prompt for entity presentation & motion (no behavior). */
  public static String step2EntitySystemPrompt() {
    return """
        You are configuring STEP 2: SpellEntity.

        The following is the config for the spell:
        {
          name: string // Edgy, wizardy spell name (2-4 words max). Should sound mystical and powerful. Examples: "Infernal Devastation", "Glacial Doom", "Ethereal Surge", "Void Cascade", "Crimson Wrath", "Spectral Lance".
          prompt: string // Short visual description for 3D model generation. Note that you do not need to include size description here as that will have no effect, that is better left for the microScale parameter. (e.g. "glowing ice shard", "fireball with smoke trail").
          microScale: number (0.01-0.05) // Visual size multiplier. 0.01= small, 0.03 = medium, 0.05 = large.
          shouldMove: boolean // If true, spell travels forward in player's look direction. If false, spawns stationary at cast location.
          speed: number (>=0) // Travel speed in blocks/tick (multiply by 20 for blocks/second). Typical values: slow=0.25 (5 b/s), medium=0.5 (10 b/s), fast=1.0 (20 b/s), very fast=1.5+ (30+ b/s). Ignored if shouldMove=false.
          cooldownSeconds: number (1.0-30.0) // Cooldown duration in seconds before the spell can be cast again. Balance this based on the spell's power: weak/utility spells = 1-5s, moderate damage/effects = 5-10s, powerful spells = 10-20s, ultimate spells = 20-30s. Consider damage, radius, status effects, and knockback when deciding.
        }
        """;
  }

  /** STEP 2: Notepad prompt for entity reasoning. */
  public static String step2EntityNotepadPrompt(String playerIntent) {
    return """
        STEP 2 Notepad, use this to think and plan the spell and the entity of the spell given the player intent:
        - Player intent: "%s"

        - Give justifications for your choices
        - Account for synergy with the behavior of the spell decided in STEP 1.
        """
        .formatted(playerIntent);
  }

  /** Final instruction for JSON-only output (used in all steps). */
  public static String finalJsonOnlyInstruction() {
    return """
        Now output ONLY the final JSON object as specified. \
        Do NOT include any prose, markdown, or code fences/backticks. \
        Return pure JSON only: the first character MUST be '{' and the last MUST be '}'. \
        Example format (keys will differ): {"exampleKey": 1}""";
  }
}
