package com.mrfuzzihead.fuzziids.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import com.mrfuzzihead.fuzziids.Config;

public enum Mixins implements IMixins {

    MINECRAFT(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins(
            "BiomeGenBaseMixin",
            "DataWatcherMixin",
            "DimensionManagerMixin",
            "EnchantmentMixin",
            "EntityListMixin",
            "PotionMixin")
        .setApplyIf(() -> Config.mainEnable));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
