package com.hadroncfy.sreplay.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ThreadedAnvilChunkStorageAccessor {
    @Accessor("viewDistance")
    int getViewDistance();

    @Invoker("updateChunkTracking")
    void sendWatchPackets2(ServerPlayer player, ChunkPos pos, Packet<?>[] packets, boolean withinMaxWatchDistance, boolean withinViewDistance);
}