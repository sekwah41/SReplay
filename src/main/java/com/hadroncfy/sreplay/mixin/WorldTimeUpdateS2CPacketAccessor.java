package com.hadroncfy.sreplay.mixin;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetTimePacket.class)
public interface WorldTimeUpdateS2CPacketAccessor {
    @Accessor("gameTime")
    long getGameTime();
}