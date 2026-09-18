package com.mrfuzzihead.fuzziids.ids;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * Resolves the *actual* ID capacity for each category at dump time. Capacities are read from
 * the live registries wherever possible, which automatically reflects whatever ID-extending
 * mod is installed (EndlessIDs, NotEnoughIDs, ...) and its per-module config. Only the
 * map-based registries (entities, data watchers) need EndlessIDs' constants, read reflectively
 * so this mod has no hard dependency on it.
 */
public final class LimitResolver {

    private static final String EID_CONSTANTS_CLASS = "com.falsepattern.endlessids.constants.ExtendedConstants";

    private static Boolean endlessIdsPresent;

    private LimitResolver() {}

    public static boolean isEndlessIdsPresent() {
        Boolean present = endlessIdsPresent;
        if (present == null) {
            try {
                Class.forName(EID_CONSTANTS_CLASS, false, LimitResolver.class.getClassLoader());
                present = Boolean.TRUE;
            } catch (Throwable throwable) {
                present = Boolean.FALSE;
            }
            endlessIdsPresent = present;
        }
        return present.booleanValue();
    }

    /** Reads a public static int constant from EndlessIDs' {@link #EID_CONSTANTS_CLASS}, if loaded. */
    public static int eidIntConstant(String fieldName, int fallback) {
        try {
            Class<?> constants = Class.forName(EID_CONSTANTS_CLASS, true, LimitResolver.class.getClassLoader());
            return constants.getField(fieldName)
                .getInt(null);
        } catch (Throwable throwable) {
            return fallback;
        }
    }

    /** @return the ID capacity, or -1 if unbounded (dimensions). */
    public static int capacity(IdCategory category) {
        switch (category) {
            case BIOMES:
                return BiomeGenBase.getBiomeGenArray().length;
            case POTIONS:
                return Potion.potionTypes.length;
            case ENCHANTMENTS:
                return Enchantment.enchantmentsList.length;
            case ENTITIES: {
                int max = eidIntConstant("maxEntityID", -1);
                return max >= 0 ? max + 1 : 256;
            }
            case DATAWATCHERS: {
                int max = eidIntConstant("maxWatchableID", -1);
                return max >= 0 ? max + 1 : 32;
            }
            case DIMENSIONS:
                return -1;
            default:
                return -1;
        }
    }

    public static String limitSource(IdCategory category) {
        switch (category) {
            case BIOMES:
                return "BiomeGenBase.getBiomeGenArray().length";
            case POTIONS:
                return "Potion.potionTypes.length";
            case ENCHANTMENTS:
                return "Enchantment.enchantmentsList.length";
            case ENTITIES:
                return isEndlessIdsPresent() ? "EndlessIDs ExtendedConstants.maxEntityID + 1"
                    : "vanilla EntityList limit (256)";
            case DATAWATCHERS:
                return isEndlessIdsPresent() ? "EndlessIDs ExtendedConstants.maxWatchableID + 1"
                    : "vanilla DataWatcher limit (32)";
            case DIMENSIONS:
                return "unbounded (DimensionManager int registry)";
            default:
                return "unknown";
        }
    }
}
