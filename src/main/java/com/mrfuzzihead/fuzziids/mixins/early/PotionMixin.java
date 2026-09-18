package com.mrfuzzihead.fuzziids.mixins.early;

import net.minecraft.potion.Potion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzziids.ids.IdCategory;
import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.ModOwnerResolver;

/**
 * Captures every potion registration. Vanilla silently overwrites conflicting slots, so the
 * previous occupant is snapshotted at HEAD and reported as a conflict at RETURN.
 */
@Mixin(Potion.class)
public abstract class PotionMixin {

    /**
     * Lazily initialized: mixin-declared static fields are initialized at the end of the target
     * class's {@code <clinit>}, but the potions construct <em>during</em> that same
     * {@code <clinit>}, so the field is still null when the first constructors run.
     */
    @Unique
    private static ThreadLocal<Potion> fuzziids$previous;

    @Unique
    private static ThreadLocal<Potion> fuzziids$previousSlot() {
        ThreadLocal<Potion> slot = fuzziids$previous;
        if (slot == null) {
            slot = new ThreadLocal<Potion>();
            fuzziids$previous = slot;
        }
        return slot;
    }

    @Inject(method = "<init>(IZI)V", at = @At("HEAD"))
    private static void fuzziids$onInitHead(int id, boolean isBadEffect, int liquidColor, CallbackInfo ci) {
        try {
            if (id >= 0 && id < Potion.potionTypes.length) {
                fuzziids$previousSlot().set(Potion.potionTypes[id]);
            }
        } catch (Throwable ignored) {
            // never break potion registration
        }
    }

    @Inject(method = "<init>(IZI)V", at = @At("RETURN"))
    private void fuzziids$onInitReturn(int id, boolean isBadEffect, int liquidColor, CallbackInfo ci) {
        try {
            Potion previous = fuzziids$previousSlot().get();
            fuzziids$previousSlot().remove();
            Potion self = (Potion) (Object) this;
            String className = self.getClass()
                .getName();
            String owner = ModOwnerResolver.currentOwner();
            IdCollector.recordAttempt(IdCategory.POTIONS, id, className, className, owner);
            if (previous != null && previous != self) {
                IdCollector.recordConflict(
                    IdCategory.POTIONS,
                    id,
                    previous.getClass()
                        .getName(),
                    className,
                    owner);
            }
        } catch (Throwable ignored) {
            // never break potion registration
        }
    }
}
