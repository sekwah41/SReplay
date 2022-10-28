package com.hadroncfy.sreplay.mixin;

import com.google.common.collect.ImmutableList;
import com.hadroncfy.sreplay.recording.Photographer;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.hadroncfy.sreplay.recording.Photographer.getRealViewDistance;

@Mixin(ChunkMap.class)
public abstract class MixinThreadedAnvilChunkStorage {
    @Shadow private int viewDistance;

    /**
     * Workaround because the redirect below cant capture the local
     */
    ServerPlayer tempPlayer;
    @Inject(method = "getPlayers", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkMap;viewDistance:I"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void getPlayers(ChunkPos pos, boolean boundaryOnly, CallbackInfoReturnable<List<ServerPlayer>> cir, Set set, ImmutableList.Builder builder, Iterator var5, ServerPlayer serverPlayer, SectionPos sectionPos) {
        this.tempPlayer = serverPlayer;
    }

    @Redirect(method = "getPlayers", at = @At(
        value = "FIELD",
        target = "Lnet/minecraft/server/level/ChunkMap;viewDistance:I"
    ) )
    private int getWatchDistance$lambda0$getPlayersWatchingChunk(ChunkMap instance){
        if(tempPlayer != null) {
            return getRealViewDistance(tempPlayer, viewDistance);
        }
        else {
            return viewDistance;
        }
    }

    // TODO fix
//    @Redirect(method = "method_17219", at = @At(
//        value = "FIELD",
//        target = "Lnet/minecraft/server/level/ChunkMap;viewDistance:I"
//    ))
//    private int getWatchDistance$lambda0$setViewDistance(ChunkMap instance) {
//        return getRealViewDistance(tempPlayer, viewDistance);
//    }
    
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