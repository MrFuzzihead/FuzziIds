package com.mrfuzzihead.fuzziids.ids;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/**
 * Resolves which mod is currently registering an ID. Must stay loadable from early mixin
 * handlers, hence the defensive try/catch blocks.
 */
public final class ModOwnerResolver {

    private ModOwnerResolver() {}

    public static String currentOwner() {
        try {
            Loader loader = Loader.instance();
            if (loader == null) {
                return "minecraft";
            }
            ModContainer container = loader.activeModContainer();
            if (container == null) {
                return "minecraft";
            }
            return container.getModId();
        } catch (Throwable throwable) {
            // FML is not up yet: everything registered so far is vanilla/bootstrap code.
            return "minecraft";
        }
    }
}
