package com.hadroncfy.sreplay.recording;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;

public class HackyClientConnection extends Connection {
    private IPacketListener p;

    public HackyClientConnection(PacketFlow networkSide, IPacketListener p) {
        super(networkSide);
        this.p = p;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void notifyListener(Future future, GenericFutureListener l) {
        try {
            l.operationComplete(future);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
        p.onPacket(packet);
        if (listener != null) {
            try {
                listener.onSuccess();
                // TODO find out how this relates to the callbacks
                //notifyListener(new SimpleCompletedFuture(), callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isMemoryConnection() {
        return true;
    }
}