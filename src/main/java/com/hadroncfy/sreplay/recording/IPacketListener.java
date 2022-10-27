package com.hadroncfy.sreplay.recording;

import net.minecraft.network.protocol.Packet;

public interface IPacketListener {
    void onPacket(Packet<?> p);
}