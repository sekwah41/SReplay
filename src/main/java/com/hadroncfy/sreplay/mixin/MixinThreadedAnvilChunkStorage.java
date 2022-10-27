package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.recording.Photographer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.hadroncfy.sreplay.recording.Photographer.getRealViewDistance;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {
    @Shadow private int viewDistance;

    @Shadow
    private static int checkerboardDistance(ChunkPos pos, ServerPlayer player, boolean useCameraPosition){ return 0; }

    @Redirect(method = "method_18707", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/server/level/ChunkMap;viewDistance:I"
    ))
    private int getWatchDistance$lambda0$getPlayersWatchingChunk(ChunkMap cela, ChunkPos pos, boolean bl, ServerPlayer player){
        return getRealViewDistance(player, viewDistance);
    }

    @Redirect(method = "method_17219", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/server/level/ChunkMap;viewDistance:I"
    ))
    private int getWatchDistance$lambda0$setViewDistance(ChunkMap cela, ChunkPos pos, int previousViewDistance, Packet<?>[] packets, ServerPlayer player){
        return getRealViewDistance(player, viewDistance);
    }
    
    @Redirect(method = "move", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/server/level/ChunkMap;viewDistance:I"
    ))
    private int getCurrentWatchDistance(ChunkMap cela, ServerPlayer player) {
        if (player instanceof Photographer){
            return ((Photographer)player).getCurrentWatchDistance();
        }
        return this.viewDistance;
    }
}