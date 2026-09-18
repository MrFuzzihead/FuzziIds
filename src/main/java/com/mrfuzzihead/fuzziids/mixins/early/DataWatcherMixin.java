package com.mrfuzzihead.fuzziids.mixins.early;

import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.ModOwnerResolver;

/**
 * Records every data watcher slot per entity class. Only the first instance of each entity
 * class is recorded (watcher slots are class-level), keeping the hot path down to one
 * ConcurrentHashMap lookup per entity construction.
 */
@Mixin(DataWatcher.class)
public abstract class DataWatcherMixin {

    @Shadow
    @Final
    private Entity field_151511_a;

    @Inject(method = "addObjectByDataType(II)V", at = @At("RETURN"))
    private void fuzziids$onAddObjectByDataType(int id, int dataType, CallbackInfo ci) {
        try {
            Entity owner = this.field_151511_a;
            if (owner == null) {
                return;
            }
            String ownerClass = owner.getClass()
                .getName();
            if (!IdCollector.tryRecordWatcherClass(ownerClass)) {
                return;
            }
            String declaringClass = fuzziids$findDeclaringClass();
            String ownerMod = ModOwnerResolver.currentOwner();
            IdCollector.recordWatcher(ownerClass, id, dataType, declaringClass, ownerMod);
        } catch (Throwable ignored) {
            // never break data watcher registration
        }
    }

    /**
     * The immediate caller of the data watcher registration. Note: {@code Thread.getStackTrace}
     * includes a frame for itself ({@code java.lang.Thread}), which must be skipped as well.
     */
    @Unique
    private static String fuzziids$findDeclaringClass() {
        StackTraceElement[] stack = Thread.currentThread()
            .getStackTrace();
        String dataWatcherName = DataWatcher.class.getName();
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (className.equals(dataWatcherName) || className.equals("java.lang.Thread")) {
                continue;
            }
            return className;
        }
        return "unknown";
    }
}
