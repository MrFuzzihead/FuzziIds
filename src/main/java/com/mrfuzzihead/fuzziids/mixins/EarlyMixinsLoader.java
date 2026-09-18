package com.mrfuzzihead.fuzziids.mixins;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.mrfuzzihead.fuzziids.Config;
import com.mrfuzzihead.fuzziids.FuzziIds;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinsLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getMixinConfig() {
        return "mixins.fuzziids.early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        Config.synchronizeConfiguration(
            new File(Launch.minecraftHome, "config/" + FuzziIds.MODID + "/" + FuzziIds.MODID + ".cfg"));
        return IMixins.getEarlyMixins(Mixins.class, loadedCoreMods);
    }
}
