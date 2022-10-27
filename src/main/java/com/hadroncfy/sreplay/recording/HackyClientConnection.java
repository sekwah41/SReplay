package com.hadroncfy.sreplay.recording;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

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
    public void send(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> callback) {
        p.onPacket(packet);
        if (callback != null) {
            try {
                notifyListener(new SimpleCompletedFuture(), callback);
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