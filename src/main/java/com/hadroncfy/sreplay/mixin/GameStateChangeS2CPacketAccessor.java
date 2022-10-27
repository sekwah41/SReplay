package com.hadroncfy.sreplay.mixin;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundGameEventPacket.class)
public interface GameStateChangeS2CPacketAccessor {
    @Accessor("event")
    ClientboundGameEventPacket.Type getEvent();
}