package com.mrfuzzihead.fuzziids.ids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central, dependency-free storage for all ID registration events captured by the early
 * mixins. This class must never reference Minecraft/FML classes: it is loaded while early
 * mixin targets (e.g. {@code BiomeGenBase}) are being class-initialized.
 */
public final class IdCollector {

    private IdCollector() {}

    /** Registration attempt captured by a mixin (name may be provisional at ctor time). */
    public static final class OwnerInfo {

        public final String nameHint;
        public final String className;
        public final String ownerMod;

        OwnerInfo(String nameHint, String className, String ownerMod) {
            this.nameHint = nameHint;
            this.className = className;
            this.ownerMod = ownerMod;
        }
    }

    /** A detected conflict between two registrations for the same ID. */
    public static final class Conflict {

        public final IdCategory category;
        public final int id;
        public final String existing;
        public final String attempted;
        public final String ownerMod;

        public Conflict(IdCategory category, int id, String existing, String attempted, String ownerMod) {
            this.category = category;
            this.id = id;
            this.existing = existing;
            this.attempted = attempted;
            this.ownerMod = ownerMod;
        }
    }

    /** One data watcher slot recorded for an entity class. */
    public static final class WatcherEntry {

        public final int watcherId;
        public final int dataType;
        public final String declaringClass;
        public final String ownerMod;

        WatcherEntry(int watcherId, int dataType, String declaringClass, String ownerMod) {
            this.watcherId = watcherId;
            this.dataType = dataType;
            this.declaringClass = declaringClass;
            this.ownerMod = ownerMod;
        }
    }

    /** One dimension or provider-type registration. */
    public static final class DimensionEntry {

        public final int id;
        public final int providerType;
        public final String providerClass;
        public final boolean keepLoaded;
        public final String ownerMod;

        DimensionEntry(int id, int providerType, String providerClass, boolean keepLoaded, String ownerMod) {
            this.id = id;
            this.providerType = providerType;
            this.providerClass = providerClass;
            this.keepLoaded = keepLoaded;
            this.ownerMod = ownerMod;
        }
    }

    private static final Map<IdCategory, Map<Integer, OwnerInfo>> CAPTURES = new EnumMap<IdCategory, Map<Integer, OwnerInfo>>(
        IdCategory.class);

    private static final List<Conflict> CONFLICTS = new ArrayList<Conflict>();

    /** Entity classes whose watcher slots have already been recorded (first instance wins). */
    private static final Set<String> RECORDED_WATCHER_CLASSES = Collections
        .newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /** owner entity class -> recorded watcher slots. */
    public static final Map<String, List<WatcherEntry>> WATCHERS = new ConcurrentHashMap<String, List<WatcherEntry>>();

    /** provider type id -> entry. */
    public static final Map<Integer, DimensionEntry> PROVIDERS = new ConcurrentHashMap<Integer, DimensionEntry>();

    /** dimension id -> entry. */
    public static final Map<Integer, DimensionEntry> DIMENSIONS = new ConcurrentHashMap<Integer, DimensionEntry>();

    static {
        for (IdCategory category : IdCategory.values()) {
            CAPTURES.put(category, new LinkedHashMap<Integer, OwnerInfo>());
        }
    }

    public static synchronized void recordAttempt(IdCategory category, int id, String nameHint, String className,
        String ownerMod) {
        CAPTURES.get(category)
            .put(Integer.valueOf(id), new OwnerInfo(nameHint, className, ownerMod));
    }

    public static synchronized void recordConflict(IdCategory category, int id, String existing, String attempted,
        String ownerMod) {
        for (Conflict conflict : CONFLICTS) {
            if (conflict.category == category && conflict.id == id
                && conflict.existing.equals(existing)
                && conflict.attempted.equals(attempted)) {
                return; // identical conflict already recorded
            }
        }
        CONFLICTS.add(new Conflict(category, id, existing, attempted, ownerMod));
    }

    public static synchronized Map<IdCategory, Map<Integer, OwnerInfo>> snapshotCaptures() {
        Map<IdCategory, Map<Integer, OwnerInfo>> copy = new EnumMap<IdCategory, Map<Integer, OwnerInfo>>(
            IdCategory.class);
        for (Map.Entry<IdCategory, Map<Integer, OwnerInfo>> entry : CAPTURES.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<Integer, OwnerInfo>(entry.getValue()));
        }
        return copy;
    }

    public static synchronized List<Conflict> snapshotConflicts() {
        return new ArrayList<Conflict>(CONFLICTS);
    }

    /**
     * Marks an entity class as "watcher recording started". Returns {@code true} only for the
     * first call, so the (expensive) stack walk and recording happen once per class.
     */
    public static boolean tryRecordWatcherClass(String ownerClass) {
        return RECORDED_WATCHER_CLASSES.add(ownerClass);
    }

    public static void recordWatcher(String ownerClass, int watcherId, int dataType, String declaringClass,
        String ownerMod) {
        List<WatcherEntry> list = WATCHERS.get(ownerClass);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<WatcherEntry>());
            WATCHERS.put(ownerClass, list);
        }
        synchronized (list) {
            for (WatcherEntry entry : list) {
                if (entry.watcherId == watcherId && entry.dataType != dataType) {
                    recordConflict(
                        IdCategory.DATAWATCHERS,
                        watcherId,
                        ownerClass + " declares watcher " + watcherId + " as data type " + entry.dataType,
                        ownerClass + " re-declares watcher " + watcherId + " as data type " + dataType,
                        ownerMod);
                    break;
                }
            }
            list.add(new WatcherEntry(watcherId, dataType, declaringClass, ownerMod));
        }
    }

    public static void recordProvider(int providerTypeId, String providerClass, boolean keepLoaded, String ownerMod) {
        Integer key = Integer.valueOf(providerTypeId);
        if (!PROVIDERS.containsKey(key)) {
            PROVIDERS.put(key, new DimensionEntry(providerTypeId, providerTypeId, providerClass, keepLoaded, ownerMod));
        }
    }

    public static void recordDimension(int dimensionId, int providerType, String ownerMod) {
        Integer key = Integer.valueOf(dimensionId);
        DimensionEntry existing = DIMENSIONS.get(key);
        if (existing != null) {
            recordConflict(
                IdCategory.DIMENSIONS,
                dimensionId,
                "dimension already registered by mod '" + existing.ownerMod
                    + "' with provider type "
                    + existing.providerType,
                "provider type " + providerType,
                ownerMod);
            return;
        }
        DIMENSIONS.put(key, new DimensionEntry(dimensionId, providerType, null, false, ownerMod));
    }

    /** Dimensions are frequently un- and re-registered around world loads; keep the state accurate. */
    public static void removeDimension(int dimensionId) {
        DIMENSIONS.remove(Integer.valueOf(dimensionId));
    }
}
