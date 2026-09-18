package com.mrfuzzihead.fuzziids;

import java.io.File;

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
        Config.configDir = new File(event.getModConfigurationDirectory(), FuzziIds.MODID);
        Config.synchronizeConfiguration(new File(Config.configDir, FuzziIds.MODID + ".cfg"));

        FuzziIds.LOG
            .info("FuzziIds {} (version {})", Config.mainEnable ? "enabled" : "DISABLED via config", Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        if (!Config.mainEnable) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (Config.mainEnable && Config.dumpOnPostInit) {
            IdDumpManager.dump();
        }
    }

    public void serverStarting(FMLServerStartingEvent event) {
        if (!Config.mainEnable) {
            return;
        }
        ((ServerCommandManager) event.getServer()
            .getCommandManager()).registerCommand(new DumpIdsCommand());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (Config.mainEnable && Config.dumpOnWorldLoad) {
            IdDumpManager.dump();
        }
    }
}
