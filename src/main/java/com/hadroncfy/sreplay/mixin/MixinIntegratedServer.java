package com.hadroncfy.sreplay.mixin;

import java.io.File;
import java.net.Proxy;

import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.RegistryAccess.RegistryHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.WorldData;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer {

    private MixinIntegratedServer(Thread thread, RegistryHolder impl, LevelStorageAccess session, WorldData saveProperties,
            PackRepository resourcePackManager, Proxy proxy, DataFixer dataFixer,
            ServerResources serverResourceManager, MinecraftSessionService minecraftSessionService,
            GameProfileRepository gameProfileRepository, GameProfileCache userCache,
            ChunkProgressListenerFactory worldGenerationProgressListenerFactory) {
        super(thread, impl, session, saveProperties, resourcePackManager, proxy, dataFixer, serverResourceManager,
                minecraftSessionService, gameProfileRepository, userCache, worldGenerationProgressListenerFactory);
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"))
    public void onPause(CallbackInfo ci) {
        Photographer.listFakes(this).forEach(Photographer::onSoftPause);
    }

    @Inject(method = "halt", at = @At("HEAD"))
    public void onStop(boolean b, CallbackInfo ci){
        Photographer.killAllFakes(this, false);
    }
}