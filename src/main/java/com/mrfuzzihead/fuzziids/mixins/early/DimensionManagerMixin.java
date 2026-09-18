package com.mrfuzzihead.fuzziids.mixins.early;

import java.util.Hashtable;

import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrfuzzihead.fuzziids.ids.IdCategory;
import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.ModOwnerResolver;

/**
 * Captures dimension provider-type and dimension id registrations. Vanilla throws on
 * duplicate dimension ids and silently returns false on duplicate provider types, so both
 * are checked at HEAD against the shadowed tables.
 */
@Mixin(DimensionManager.class)
public abstract class DimensionManagerMixin {

    @Shadow(remap = false)
    private static Hashtable<Integer, Class<? extends WorldProvider>> providers;

    @Shadow(remap = false)
    private static Hashtable<Integer, Integer> dimensions;

    @Inject(method = "registerProviderType(ILjava/lang/Class;Z)Z", at = @At("HEAD"), remap = false)
    private static void fuzziids$onRegisterProviderType(int id, Class<? extends WorldProvider> provider,
        boolean keepLoaded, CallbackInfoReturnable<Boolean> cir) {
        try {
            String owner = ModOwnerResolver.currentOwner();
            IdCollector.recordProvider(id, provider.getName(), keepLoaded, owner);
            Class<?> existing = providers.get(Integer.valueOf(id));
            if (existing != null) {
                IdCollector.recordConflict(
                    IdCategory.DIMENSIONS,
                    id,
                    "provider type in use by " + existing.getName(),
                    "attempted provider " + provider.getName() + " (vanilla would refuse)",
                    owner);
            }
        } catch (Throwable ignored) {
            // never break dimension registration
        }
    }

    @Inject(method = "registerDimension(II)V", at = @At("HEAD"), remap = false)
    private static void fuzziids$onRegisterDimension(int id, int providerType, CallbackInfo ci) {
        try {
            String owner = ModOwnerResolver.currentOwner();
            IdCollector.recordDimension(id, providerType, owner);
        } catch (Throwable ignored) {
            // never break dimension registration
        }
    }
}
