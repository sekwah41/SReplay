package com.hadroncfy.sreplay.mixin;

import com.hadroncfy.sreplay.recording.Photographer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    public abstract boolean isDedicatedServer();
    
    @Inject(method = "stopServer", at = @At("HEAD"))
    public void onShutdown(CallbackInfo ci){
        if (isDedicatedServer()){
            Photographer.killAllFakes((MinecraftServer)(Object)this, false);
        }
    }
}