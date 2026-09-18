package com.mrfuzzihead.fuzziids.mixins.early;

import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzziids.ids.IdCategory;
import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.ModOwnerResolver;

/**
 * Captures every biome registration (vanilla + mods). The biome name is set after the
 * constructor runs, so the event only records a name hint; the dump-time registry scan
 * provides the final names. Conflicts are previous non-null occupants of the same slot.
 */
@Mixin(BiomeGenBase.class)
public abstract class BiomeGenBaseMixin {

    /**
     * Lazily initialized: mixin-declared static fields are initialized at the end of the target
     * class's {@code <clinit>}, but the biomes construct <em>during</em> that same
     * {@code <clinit>}, so the field is still null when the first constructors run.
     */
    @Unique
    private static ThreadLocal<BiomeGenBase> fuzziids$previous;

    @Unique
    private static ThreadLocal<BiomeGenBase> fuzziids$previousSlot() {
        ThreadLocal<BiomeGenBase> slot = fuzziids$previous;
        if (slot == null) {
            slot = new ThreadLocal<BiomeGenBase>();
            fuzziids$previous = slot;
        }
        return slot;
    }

    @Inject(method = "<init>(IZ)V", at = @At("HEAD"))
    private static void fuzziids$onInitHead(int id, boolean register, CallbackInfo ci) {
        try {
            BiomeGenBase[] list = BiomeGenBase.getBiomeGenArray();
            if (id >= 0 && id < list.length) {
                fuzziids$previousSlot().set(list[id]);
            }
        } catch (Throwable ignored) {
            // never break biome registration
        }
    }

    @Inject(method = "<init>(IZ)V", at = @At("RETURN"))
    private void fuzziids$onInitReturn(int id, boolean register, CallbackInfo ci) {
        try {
            BiomeGenBase previous = fuzziids$previousSlot().get();
            fuzziids$previousSlot().remove();
            BiomeGenBase self = (BiomeGenBase) (Object) this;
            String className = self.getClass()
                .getName();
            String owner = ModOwnerResolver.currentOwner();
            IdCollector.recordAttempt(IdCategory.BIOMES, id, className, className, owner);
            if (previous != null && previous != self) {
                String previousName = previous.biomeName != null ? previous.biomeName
                    : previous.getClass()
                        .getName();
                IdCollector.recordConflict(IdCategory.BIOMES, id, previousName, className, owner);
            }
        } catch (Throwable ignored) {
            // never break biome registration
        }
    }
}
