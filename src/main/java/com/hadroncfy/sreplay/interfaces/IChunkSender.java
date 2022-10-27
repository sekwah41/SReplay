package com.hadroncfy.sreplay.interfaces;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;

public interface IChunkSender {
    void sendChunk(ServerPlayer player, LevelChunk chunk);
}