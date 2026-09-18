package com.mrfuzzihead.fuzziids.mixins.early;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrfuzzihead.fuzziids.ids.IdCategory;
import com.mrfuzzihead.fuzziids.ids.IdCollector;
import com.mrfuzzihead.fuzziids.ids.ModOwnerResolver;

/**
 * Captures every entity registration. Only the 3-arg overload is hooked (the egg-colored
 * overload delegates to it). Vanilla logs an error and skips registration on conflict, so
 * rejected registrations are reported by the dump-time reconciliation.
 */
@Mixin(EntityList.class)
public abstract class EntityListMixin {

    @Inject(method = "addMapping(Ljava/lang/Class;Ljava/lang/String;I)V", at = @At("HEAD"))
    private static void fuzziids$onAddMapping(Class<? extends Entity> entityClass, String entityName, int id,
        CallbackInfo ci) {
        try {
            String owner = ModOwnerResolver.currentOwner();
            String newDescription = entityName + " (" + entityClass.getName() + ")";
            Object previousById = EntityList.IDtoClassMapping.get(Integer.valueOf(id));
            if (previousById != null) {
                IdCollector.recordConflict(
                    IdCategory.ENTITIES,
                    id,
                    "numeric id in use by " + ((Class<?>) previousById).getName(),
                    newDescription,
                    owner);
            }
            Object previousByName = EntityList.stringToClassMapping.get(entityName);
            if (previousByName != null) {
                IdCollector.recordConflict(
                    IdCategory.ENTITIES,
                    id,
                    "entity name '" + entityName + "' in use by " + ((Class<?>) previousByName).getName(),
                    newDescription,
                    owner);
            }
            IdCollector.recordAttempt(IdCategory.ENTITIES, id, entityName, entityClass.getName(), owner);
        } catch (Throwable ignored) {
            // never break entity registration
        }
    }
}
