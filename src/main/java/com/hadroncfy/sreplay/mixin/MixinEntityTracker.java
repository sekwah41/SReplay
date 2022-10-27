package com.hadroncfy.sreplay.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.hadroncfy.sreplay.recording.Photographer.getRealViewDistance;

import java.util.Set;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.hadroncfy.sreplay.recording.Photographer;

@Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage$EntityTracker")
public class MixinEntityTracker {
    @Shadow @Final
    private Entity entity;

    @Redirect(method = "updateCameraPosition(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;method_18725(Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;)I"
    ))
    private int getViewDistance(ChunkMap cela, ServerPlayer player){
        return getRealViewDistance(player, ((ThreadedAnvilChunkStorageAccessor)cela).getWatchDistance());
    }

    @Redirect(method = "updateCameraPosition(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At(
        value = "INVOKE",
        target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"
    ))
    private boolean onAddTrackingPlayer(Set cela, Object player){
        if (this.entity.getType() == EntityType.ITEM && player instanceof Photographer){
            Photographer p = (Photographer)player;
            if (p.isItemDisabled()){
                return false;
            }
        }
        return cela.add(player);
    }
}