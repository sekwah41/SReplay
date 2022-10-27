package com.hadroncfy.sreplay.mixin;

import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundAddPlayerPacket.class)
public interface PlayerSpawnS2CPacketAccessor {
    @Accessor("playerId")
    UUID getPlayerId();
}