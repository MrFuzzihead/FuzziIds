package com.mrfuzzihead.fuzziids.ids;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityList;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * Dump-time pass over the *live* registries. This is the authoritative source for the "used"
 * lists (registration events are only used to attribute owner mods and to detect conflicts).
 */
public final class RegistryScanner {

    private RegistryScanner() {}

    /** A live, used ID. */
    public static final class UsedEntry {

        public final int id;
        public final String name;
        public final String ownerMod;
        public final String className;

        UsedEntry(int id, String name, String ownerMod, String className) {
            this.id = id;
            this.name = name;
            this.ownerMod = ownerMod;
            this.className = className;
        }
    }

    public static Map<IdCategory, Map<Integer, UsedEntry>> collectUsed() {
        Map<IdCategory, Map<Integer, UsedEntry>> result = new EnumMap<IdCategory, Map<Integer, UsedEntry>>(
            IdCategory.class);
        for (IdCategory category : IdCategory.values()) {
            result.put(category, new TreeMap<Integer, UsedEntry>());
        }
        Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures = IdCollector.snapshotCaptures();

        scanBiomes(result, captures);
        scanPotions(result, captures);
        scanEnchantments(result, captures);
        scanEntities(result, captures);
        return result;
    }

    private static void scanBiomes(Map<IdCategory, Map<Integer, UsedEntry>> result,
        Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures) {
        BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
        for (int i = 0; i < biomes.length; i++) {
            BiomeGenBase biome = biomes[i];
            if (biome == null) {
                continue;
            }
            String name = biome.biomeName != null && !biome.biomeName.isEmpty() ? biome.biomeName
                : biome.getClass()
                    .getSimpleName();
            add(
                result,
                IdCategory.BIOMES,
                i,
                name,
                ownerOf(captures, IdCategory.BIOMES, i),
                biome.getClass()
                    .getName());
        }
    }

    private static void scanPotions(Map<IdCategory, Map<Integer, UsedEntry>> result,
        Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures) {
        Potion[] potions = Potion.potionTypes;
        for (int i = 0; i < potions.length; i++) {
            Potion potion = potions[i];
            if (potion == null) {
                continue;
            }
            add(
                result,
                IdCategory.POTIONS,
                i,
                describePotion(potion),
                ownerOf(captures, IdCategory.POTIONS, i),
                potion.getClass()
                    .getName());
        }
    }

    private static void scanEnchantments(Map<IdCategory, Map<Integer, UsedEntry>> result,
        Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures) {
        Enchantment[] enchantments = Enchantment.enchantmentsList;
        for (int i = 0; i < enchantments.length; i++) {
            Enchantment enchantment = enchantments[i];
            if (enchantment == null) {
                continue;
            }
            add(
                result,
                IdCategory.ENCHANTMENTS,
                i,
                describeEnchantment(enchantment),
                ownerOf(captures, IdCategory.ENCHANTMENTS, i),
                enchantment.getClass()
                    .getName());
        }
    }

    private static void scanEntities(Map<IdCategory, Map<Integer, UsedEntry>> result,
        Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures) {
        Map<Class<?>, String> classToName = new HashMap<Class<?>, String>();
        for (Map.Entry<String, Class<? extends net.minecraft.entity.Entity>> entry : EntityList.stringToClassMapping
            .entrySet()) {
            if (!classToName.containsKey(entry.getValue())) {
                classToName.put(entry.getValue(), entry.getKey());
            }
        }
        for (Map.Entry<Integer, Class<? extends net.minecraft.entity.Entity>> entry : EntityList.IDtoClassMapping
            .entrySet()) {
            int id = entry.getKey()
                .intValue();
            Class<?> clazz = entry.getValue();
            String name = classToName.containsKey(clazz) ? classToName.get(clazz) : clazz.getSimpleName();
            add(result, IdCategory.ENTITIES, id, name, ownerOf(captures, IdCategory.ENTITIES, id), clazz.getName());
        }
    }

    /** Reads DimensionManager's private static provider table via reflection, merged with event captures. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map<Integer, IdCollector.DimensionEntry> scanProviders() {
        Map<Integer, IdCollector.DimensionEntry> result = new TreeMap<Integer, IdCollector.DimensionEntry>();
        try {
            Field providersField = net.minecraftforge.common.DimensionManager.class.getDeclaredField("providers");
            providersField.setAccessible(true);
            Hashtable<Integer, Class<? extends net.minecraft.world.WorldProvider>> providers = (Hashtable) providersField
                .get(null);
            for (Map.Entry<Integer, Class<? extends net.minecraft.world.WorldProvider>> entry : providers.entrySet()) {
                int id = entry.getKey()
                    .intValue();
                IdCollector.DimensionEntry captured = IdCollector.PROVIDERS.get(entry.getKey());
                result.put(
                    entry.getKey(),
                    new IdCollector.DimensionEntry(
                        id,
                        id,
                        entry.getValue()
                            .getName(),
                        captured != null && captured.keepLoaded,
                        captured != null ? captured.ownerMod : "unattributed"));
            }
        } catch (Throwable ignored) {
            // fall back to event captures only
        }
        for (Map.Entry<Integer, IdCollector.DimensionEntry> entry : IdCollector.PROVIDERS.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /** Reads DimensionManager's private static dimension table via reflection, merged with event captures. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map<Integer, IdCollector.DimensionEntry> scanDimensions() {
        Map<Integer, IdCollector.DimensionEntry> result = new TreeMap<Integer, IdCollector.DimensionEntry>();
        try {
            Field dimensionsField = net.minecraftforge.common.DimensionManager.class.getDeclaredField("dimensions");
            dimensionsField.setAccessible(true);
            Hashtable<Integer, Integer> dimensions = (Hashtable) dimensionsField.get(null);
            for (Map.Entry<Integer, Integer> entry : dimensions.entrySet()) {
                int id = entry.getKey()
                    .intValue();
                IdCollector.DimensionEntry captured = IdCollector.DIMENSIONS.get(entry.getKey());
                result.put(
                    entry.getKey(),
                    new IdCollector.DimensionEntry(
                        id,
                        entry.getValue()
                            .intValue(),
                        null,
                        false,
                        captured != null ? captured.ownerMod : "unattributed"));
            }
        } catch (Throwable ignored) {
            // fall back to event captures only
        }
        for (Map.Entry<Integer, IdCollector.DimensionEntry> entry : IdCollector.DIMENSIONS.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private static void add(Map<IdCategory, Map<Integer, UsedEntry>> result, IdCategory category, int id, String name,
        String ownerMod, String className) {
        result.get(category)
            .put(Integer.valueOf(id), new UsedEntry(id, name, ownerMod, className));
    }

    private static String ownerOf(Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures, IdCategory category,
        int id) {
        IdCollector.OwnerInfo info = captures.get(category)
            .get(Integer.valueOf(id));
        return info != null ? info.ownerMod : "unattributed";
    }

    private static String describePotion(Potion potion) {
        try {
            String name = potion.getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        } catch (Throwable ignored) {}
        return potion.getClass()
            .getSimpleName();
    }

    private static String describeEnchantment(Enchantment enchantment) {
        try {
            String name = enchantment.getName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        } catch (Throwable ignored) {}
        return enchantment.getClass()
            .getSimpleName();
    }
}
