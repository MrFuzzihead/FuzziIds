package com.mrfuzzihead.fuzziids.mixins.early;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzziids.ids.IdCategory;
import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.ModOwnerResolver;

/**
 * Captures every enchantment registration. Vanilla throws on duplicate enchantment ids, so the
 * conflict (if any) must be recorded at HEAD; successful registrations are recorded at RETURN.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "<init>(IILnet/minecraft/enchantment/EnumEnchantmentType;)V", at = @At("HEAD"))
    private static void fuzziids$onInitHead(int effectId, int weight, EnumEnchantmentType type, CallbackInfo ci) {
        try {
            if (effectId < 0 || effectId >= Enchantment.enchantmentsList.length) {
                return;
            }
            Enchantment previous = Enchantment.enchantmentsList[effectId];
            if (previous != null) {
                IdCollector.recordConflict(
                    IdCategory.ENCHANTMENTS,
                    effectId,
                    previous.getClass()
                        .getName(),
                    "attempted " + fuzziids$findAttemptedClass() + " (vanilla would now throw)",
                    ModOwnerResolver.currentOwner());
            }
        } catch (Throwable ignored) {
            // never break enchantment registration
        }
    }

    /**
     * The registering class is the caller of the constructor (the subclass static initializer),
     * since the handler runs before the instance exists.
     */
    @Unique
    private static String fuzziids$findAttemptedClass() {
        StackTraceElement[] stack = Thread.currentThread()
            .getStackTrace();
        String enchantmentName = Enchantment.class.getName();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className.equals(enchantmentName)) {
                continue;
            }
            return className;
        }
        return "unknown";
    }

    @Inject(method = "<init>(IILnet/minecraft/enchantment/EnumEnchantmentType;)V", at = @At("RETURN"))
    private void fuzziids$onInitReturn(int effectId, int weight, EnumEnchantmentType type, CallbackInfo ci) {
        try {
            String className = ((Object) this).getClass()
                .getName();
            IdCollector.recordAttempt(
                IdCategory.ENCHANTMENTS,
                effectId,
                className,
                className,
                ModOwnerResolver.currentOwner());
        } catch (Throwable ignored) {
            // never break enchantment registration
        }
    }
}
