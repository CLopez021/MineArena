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

  // ---- Status effect ID examples ----
  private static final String STATUS_EFFECT_EXAMPLES =
      """
      minecraft:absorption, minecraft:bad_omen, minecraft:raid_omen, minecraft:trial_omen, minecraft:bad_luck, minecraft:luck, minecraft:blindness, minecraft:darkness, minecraft:conduit_power, minecraft:dolphins_grace, minecraft:fire_resistance, minecraft:glowing, minecraft:haste, minecraft:mining_fatigue, minecraft:health_boost, minecraft:hero_of_the_village, minecraft:hunger, minecraft:infested, minecraft:oozing, minecraft:weaving, minecraft:wind_charged, minecraft:instant_damage, minecraft:instant_health, minecraft:saturation, minecraft:invisibility, minecraft:jump_boost, minecraft:levitation, minecraft:nausea, minecraft:night_vision, minecraft:poison, minecraft:wither, minecraft:regeneration, minecraft:resistance, minecraft:slow_falling, minecraft:slowness, minecraft:speed, minecraft:strength, minecraft:water_breathing, minecraft:weakness.
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
          spawnId: string // Minecraft entity ID to spawn. Use "" for none. Examples: %s
          spawnCount: number (>=0) // Number of entities to spawn at random positions within radius. Ignored if spawnId is "".
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
        - Empty spawnId or statusEffectId means "none" for that effect.
        - All effects (damage, knockback, status) have 1-second cooldown to prevent spam triggering."""
        .formatted(SPAWN_ID_EXAMPLES, STATUS_EFFECT_EXAMPLES);
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
        You are configuring STEP 2: SpellEntity (visual + movement).

        The following is the config for the spell:
        {
          prompt: string // Short visual description for 3D model generation (e.g. "glowing ice shard", "fireball with smoke trail").
          microScale: number (0.2-1.5) // Visual size multiplier. 0.5=half block, 1.0=one block, 1.5=1.5 blocks. Keep projectiles small (0.3-0.6).
          shouldMove: boolean // If true, spell travels forward in player's look direction. If false, spawns stationary at cast location.
          speed: number (>=0) // Travel speed in blocks/tick (multiply by 20 for blocks/second). Typical values: slow=0.25 (5 b/s), medium=0.5 (10 b/s), fast=1.0 (20 b/s), very fast=1.5+ (30+ b/s). Ignored if shouldMove=false.
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
