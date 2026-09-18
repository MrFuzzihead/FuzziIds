package com.mrfuzzihead.fuzziids;

import net.minecraft.command.ServerCommandManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.mrfuzzihead.fuzziids.csv.IdDumpManager;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        Config.configDir = event.getModConfigurationDirectory();

        FuzziIds.LOG.info("FuzziIds at version " + Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Config.dumpOnPostInit) {
            IdDumpManager.dump();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        ((ServerCommandManager) event.getServer()
            .getCommandManager()).registerCommand(new DumpIdsCommand());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (Config.dumpOnWorldLoad) {
            IdDumpManager.dump();
        }
    }
}
