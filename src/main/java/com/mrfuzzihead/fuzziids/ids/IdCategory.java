package com.mrfuzzihead.fuzziids.ids;

public enum IdCategory {

    BIOMES("biomes"),
    POTIONS("potions"),
    ENCHANTMENTS("enchantments"),
    ENTITIES("entities"),
    DATAWATCHERS("datawatchers"),
    DIMENSIONS("dimensions");

    /** Base file name used for the CSV reports of this category. */
    public final String fileName;

    IdCategory(String fileName) {
        this.fileName = fileName;
    }
}
