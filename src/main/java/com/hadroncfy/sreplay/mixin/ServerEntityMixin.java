package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.recording.Photographer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {

    @Shadow @Final private Entity entity;

    @Shadow public abstract void removePairing(ServerPlayer player);

    @Inject(method = "removePairing", at = @At("HEAD"))
    public void removePairing(ServerPlayer player, CallbackInfo ci) {
        if(player instanceof Photographer photographer) {
            if (isRealPlayer(entity)) {
                photographer.trackedPlayers.remove(entity);
                photographer.updatePause();
            }
        }
    }

    @Inject(method = "addPairing", at = @At("HEAD"))
    public void addPairing(ServerPlayer player, CallbackInfo ci) {
        if(player instanceof Photographer photographer) {
            if (isRealPlayer(entity)) {
                photographer.trackedPlayers.add(entity);
                photographer.updatePause();
            }
        }
    }

    private static boolean isRealPlayer(Entity entity) {
        return entity.getClass() == ServerPlayer.class;
    }

}
