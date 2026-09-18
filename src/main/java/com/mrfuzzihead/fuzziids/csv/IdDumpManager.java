package com.mrfuzzihead.fuzziids.csv;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.mrfuzzihead.fuzziids.Config;
import com.mrfuzzihead.fuzziids.FuzziIds;
import com.mrfuzzihead.fuzziids.ids.IdCategory;
import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.LimitResolver;
import com.mrfuzzihead.fuzziids.ids.RegistryScanner;

/** Writes all ID reports (used / available / conflicts / limits) as CSV files. */
public final class IdDumpManager {

    private IdDumpManager() {}

    public static void dump() {
        if (!Config.mainEnable) {
            FuzziIds.LOG.info("FuzziIds is disabled (mainEnable=false); skipping ID report dump");
            return;
        }
        File dir = Config.configDir;
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Could not create " + dir);
            }
            Map<IdCategory, Map<Integer, RegistryScanner.UsedEntry>> used = RegistryScanner.collectUsed();
            List<IdCollector.Conflict> conflicts = IdCollector.snapshotConflicts();
            appendRejectedCaptures(conflicts, used);

            for (IdCategory category : IdCategory.values()) {
                if (category == IdCategory.DIMENSIONS || category == IdCategory.DATAWATCHERS) {
                    continue;
                }
                writeUsed(dir, category, used.get(category));
                int capacity = LimitResolver.capacity(category);
                writeAvailable(
                    dir,
                    category,
                    used.get(category)
                        .keySet(),
                    capacity);
            }
            writeDataWatchers(dir);
            writeDimensions(dir);
            writeProviders(dir);
            writeConflicts(dir, conflicts);
            writeLimits(dir, used);
            FuzziIds.LOG.info("FuzziIds wrote ID reports to {}", dir.getAbsolutePath());
        } catch (Throwable throwable) {
            FuzziIds.LOG.error("FuzziIds failed to dump ID reports to {}", dir.getAbsolutePath(), throwable);
        }
    }

    private static void writeUsed(File dir, IdCategory category, Map<Integer, RegistryScanner.UsedEntry> entries)
        throws IOException {
        CsvWriter writer = new CsvWriter(new File(dir, category.fileName + ".csv"));
        try {
            writer.writeRow("id", "name", "owner_mod", "java_class");
            for (RegistryScanner.UsedEntry entry : entries.values()) {
                writer.writeRow(entry.id, entry.name, entry.ownerMod, entry.className);
            }
        } finally {
            writer.close();
        }
    }

    private static void writeAvailable(File dir, IdCategory category, Set<Integer> usedIds, int capacity)
        throws IOException {
        if (capacity < 0) {
            return;
        }
        TreeSet<Integer> usedInRange = new TreeSet<Integer>();
        for (Integer id : usedIds) {
            if (id.intValue() >= 0 && id.intValue() < capacity) {
                usedInRange.add(id);
            }
        }
        CsvWriter writer = new CsvWriter(new File(dir, category.fileName + "_available.csv"));
        try {
            writer.writeRow("start_id", "end_id");
            int previous = -1;
            for (int id : usedInRange) {
                if (id > previous + 1) {
                    emitRange(writer, previous + 1, id - 1);
                }
                previous = id;
            }
            if (previous + 1 < capacity) {
                emitRange(writer, previous + 1, capacity - 1);
            }
        } finally {
            writer.close();
        }
    }

    private static void emitRange(CsvWriter writer, int start, int end) throws IOException {
        if (Config.availableAsRanges) {
            writer.writeRow(start, end);
        } else {
            for (int id = start; id <= end; id++) {
                writer.writeRow(id, id);
            }
        }
    }

    private static void writeDataWatchers(File dir) throws IOException {
        Set<Integer> usedGlobally = new HashSet<Integer>();
        CsvWriter writer = new CsvWriter(new File(dir, IdCategory.DATAWATCHERS.fileName + ".csv"));
        try {
            writer.writeRow("owner_class", "watcher_id", "data_type", "declaring_class", "owner_mod");
            for (Map.Entry<String, List<IdCollector.WatcherEntry>> classEntry : IdCollector.WATCHERS.entrySet()) {
                String ownerClass = classEntry.getKey();
                for (IdCollector.WatcherEntry entry : classEntry.getValue()) {
                    writer.writeRow(ownerClass, entry.watcherId, entry.dataType, entry.declaringClass, entry.ownerMod);
                    usedGlobally.add(Integer.valueOf(entry.watcherId));
                }
            }
        } finally {
            writer.close();
        }
        int capacity = LimitResolver.capacity(IdCategory.DATAWATCHERS);
        writeAvailable(dir, IdCategory.DATAWATCHERS, usedGlobally, capacity);
    }

    private static void writeDimensions(File dir) throws IOException {
        Map<Integer, IdCollector.DimensionEntry> dimensions = RegistryScanner.scanDimensions();
        Map<Integer, IdCollector.DimensionEntry> providers = RegistryScanner.scanProviders();
        CsvWriter writer = new CsvWriter(new File(dir, IdCategory.DIMENSIONS.fileName + ".csv"));
        try {
            writer.writeRow("dimension_id", "provider_type_id", "provider_class", "keep_loaded", "owner_mod");
            for (IdCollector.DimensionEntry entry : dimensions.values()) {
                IdCollector.DimensionEntry provider = providers.get(Integer.valueOf(entry.providerType));
                writer.writeRow(
                    entry.id,
                    entry.providerType,
                    provider != null ? provider.providerClass : entry.providerClass != null ? entry.providerClass : "?",
                    provider != null ? provider.keepLoaded : entry.keepLoaded,
                    entry.ownerMod);
            }
        } finally {
            writer.close();
        }
    }

    private static void writeProviders(File dir) throws IOException {
        Map<Integer, IdCollector.DimensionEntry> providers = RegistryScanner.scanProviders();
        CsvWriter writer = new CsvWriter(new File(dir, "providers.csv"));
        try {
            writer.writeRow("provider_type_id", "provider_class", "keep_loaded", "owner_mod");
            for (IdCollector.DimensionEntry entry : providers.values()) {
                writer.writeRow(entry.id, entry.providerClass, entry.keepLoaded, entry.ownerMod);
            }
        } finally {
            writer.close();
        }
    }

    private static void writeConflicts(File dir, List<IdCollector.Conflict> conflicts) throws IOException {
        CsvWriter writer = new CsvWriter(new File(dir, "conflicts.csv"));
        try {
            writer.writeRow("category", "id", "existing", "attempted", "owner_mod");
            for (IdCollector.Conflict conflict : conflicts) {
                writer.writeRow(
                    conflict.category.fileName,
                    conflict.id,
                    conflict.existing,
                    conflict.attempted,
                    conflict.ownerMod);
            }
        } finally {
            writer.close();
        }
    }

    private static void writeLimits(File dir, Map<IdCategory, Map<Integer, RegistryScanner.UsedEntry>> used)
        throws IOException {
        CsvWriter writer = new CsvWriter(new File(dir, "limits.csv"));
        try {
            writer.writeRow("category", "capacity", "used", "free", "limit_source");
            for (IdCategory category : IdCategory.values()) {
                int capacity = LimitResolver.capacity(category);
                if (category == IdCategory.DATAWATCHERS) {
                    Set<Integer> ids = new HashSet<Integer>();
                    for (List<IdCollector.WatcherEntry> entries : IdCollector.WATCHERS.values()) {
                        for (IdCollector.WatcherEntry entry : entries) {
                            ids.add(Integer.valueOf(entry.watcherId));
                        }
                    }
                    writer.writeRow(
                        category.fileName,
                        capacity,
                        ids.size(),
                        capacity - ids.size(),
                        LimitResolver.limitSource(category));
                } else if (category == IdCategory.DIMENSIONS) {
                    writer.writeRow(
                        category.fileName,
                        "unbounded",
                        RegistryScanner.scanDimensions()
                            .size(),
                        "",
                        LimitResolver.limitSource(category));
                } else {
                    int usedCount = used.get(category)
                        .size();
                    writer.writeRow(
                        category.fileName,
                        capacity,
                        usedCount,
                        capacity - usedCount,
                        LimitResolver.limitSource(category));
                }
            }
        } finally {
            writer.close();
        }
    }

    /** Captured registrations that are not in the live registry were rejected or overwritten. */
    private static void appendRejectedCaptures(List<IdCollector.Conflict> conflicts,
        Map<IdCategory, Map<Integer, RegistryScanner.UsedEntry>> used) {
        Map<IdCategory, Map<Integer, IdCollector.OwnerInfo>> captures = IdCollector.snapshotCaptures();
        for (IdCategory category : IdCategory.values()) {
            if (category == IdCategory.DIMENSIONS || category == IdCategory.DATAWATCHERS) {
                continue;
            }
            for (Map.Entry<Integer, IdCollector.OwnerInfo> entry : captures.get(category)
                .entrySet()) {
                if (!used.get(category)
                    .containsKey(entry.getKey())) {
                    IdCollector.OwnerInfo info = entry.getValue();
                    conflicts.add(
                        new IdCollector.Conflict(
                            category,
                            entry.getKey()
                                .intValue(),
                            "not present in final registry",
                            "captured registration of " + info.nameHint
                                + " by mod '"
                                + info.ownerMod
                                + "' was rejected or overwritten",
                            info.ownerMod));
                }
            }
        }
    }
}
