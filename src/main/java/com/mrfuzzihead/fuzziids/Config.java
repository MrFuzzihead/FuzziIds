package com.mrfuzzihead.fuzziids;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** The folder holding both the config file and the generated CSV reports. */
    public static File configDir = new File("config/" + FuzziIds.MODID);

    public static boolean mainEnable = true;
    public static boolean dumpOnPostInit = true;
    public static boolean dumpOnWorldLoad = true;
    public static boolean availableAsRanges = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        mainEnable = configuration.getBoolean(
            "mainEnable",
            Configuration.CATEGORY_GENERAL,
            mainEnable,
            "Main toggle to enable/disable the mod. When false, no mixins are applied, no reports are"
                + " written automatically and the /fuzziids command is disabled.");
        dumpOnPostInit = configuration.getBoolean(
            "dumpOnPostInit",
            Configuration.CATEGORY_GENERAL,
            dumpOnPostInit,
            "Write the ID reports during FML post-initialization");
        dumpOnWorldLoad = configuration.getBoolean(
            "dumpOnWorldLoad",
            Configuration.CATEGORY_GENERAL,
            dumpOnWorldLoad,
            "Re-write the ID reports whenever a world loads (catches late dimension registrations)");
        availableAsRanges = configuration.getBoolean(
            "availableAsRanges",
            Configuration.CATEGORY_GENERAL,
            availableAsRanges,
            "true: *_available.csv contains start/end ranges; false: one row per free ID");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
