package com.hadroncfy.sreplay.recording.mcpr;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public class SReplayFile implements IReplayFile {
    private static final String RECORDING_FILE = "recording.tmcpr";
    private static final String RECORDING_FILE_CRC32 = "recording.tmcpr.crc32";
    private static final String MARKER_FILE = "markers.json";
    private static final String META_FILE = "metaData.json";

    private static final Gson MARKER_GSON = new GsonBuilder()
        .registerTypeAdapter(Marker.class, new Marker.Serializer())
        .create();

    private static final Gson META_GSON = new GsonBuilder()
        .registerTypeAdapter(UUID.class, new UUIDSerializer())
        .create();

    private final File tmpDir;
    private final DataOutputStream packetStream;
    private final CRC32 crc32 = new CRC32();
    private long recordedSize = 0;

    private long savedSize = 0;
    private long totalSize = 0;
    private int lastPercent;
    private ProgressBar listener;

    private final File packetFile;
    private final File markerFile;
    private final File metaFile;

    public SReplayFile(File name) throws IOException {
        this.tmpDir = new File(name.getParentFile(), name.getName() + ".tmp");
        if (tmpDir.exists()) {
            throw new IOException("recording file " + name.toString() + " already exists!");
        } else if (!tmpDir.mkdirs()) {
            throw new IOException("Failed to create temp directory for recording " + tmpDir.toString());
        }

        packetFile = new File(tmpDir, RECORDING_FILE);
        markerFile = new File(tmpDir, MARKER_FILE);
        metaFile = new File(tmpDir, META_FILE);

        packetStream = new DataOutputStream(
                new DigestOutputStream(new BufferedOutputStream(new FileOutputStream(packetFile)), crc32));
    }

    private byte[] getPacketBytes(Packet<?> packet, boolean isLogin) throws Exception {
        ConnectionProtocol nstate = isLogin ? ConnectionProtocol.LOGIN : ConnectionProtocol.PLAY;
        int packetID = nstate.getPacketId(PacketFlow.CLIENTBOUND, packet);
        ByteBuf bbuf = Unpooled.buffer();
        FriendlyByteBuf packetBuf = new FriendlyByteBuf(bbuf);
        packetBuf.writeVarInt(packetID);
        packet.write(packetBuf);

        bbuf.readerIndex(0);
        byte[] ret = new byte[bbuf.readableBytes()];
        bbuf.readBytes(ret);
        bbuf.release();
        return ret;
    }

    @Override
    public void saveMetaData(Metadata data) throws IOException {
        data.fileFormat = "MCPR";
        data.fileFormatVersion = Metadata.CURRENT_FILE_FORMAT_VERSION;
        data.protocol = SharedConstants.getCurrentVersion().getProtocolVersion();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(metaFile), StandardCharsets.UTF_8)){
            writer.write(META_GSON.toJson(data));
        }
    }

    @Override
    public void saveMarkers(List<Marker> markers) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(markerFile), StandardCharsets.UTF_8)){
            writer.write(MARKER_GSON.toJson(markers));
        }
    }

    @Override
    public void savePacket(long timestamp, Packet<?> packet, boolean isLoginPhase) throws Exception {
        byte[] data = getPacketBytes(packet, isLoginPhase);
        packetStream.writeInt((int)timestamp);
        packetStream.writeInt(data.length);
        packetStream.write(data);
        recordedSize += data.length + 8;
    }

    @Override
    public long getRecordedBytes() {
        return recordedSize;
    }

    @Override
    public synchronized void closeAndSave(File file, ProgressBar listener) throws IOException {
        packetStream.close();
        this.listener = listener;

        totalSize = 0;
        for (String fileName: tmpDir.list()){
            File f = new File(tmpDir, fileName);
            totalSize += f.length();
        }
        if (listener != null){
            listener.onStart();
        }
        
        try (ZipOutputStream os = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))){
            for (String fileName: tmpDir.list()){
                os.putNextEntry(new ZipEntry(fileName));
                File f = new File(tmpDir, fileName);
                copy(new FileInputStream(f), os);
            }
            
            os.putNextEntry(new ZipEntry(RECORDING_FILE_CRC32));
            Writer writer = new OutputStreamWriter(os);
            writer.write(Long.toString(crc32.getValue()));
            writer.flush();
        }

        for (String fileName: tmpDir.list()){
            File f = new File(tmpDir, fileName);
            Files.delete(f.toPath());
        }
        Files.delete(tmpDir.toPath());

        if (listener != null){
            listener.onDone();
        }
    }

    private void updateProgress(){
        if (listener != null){
            float percent = (float)savedSize / (float)totalSize;
            int ip = (int)(100 * percent);
            if (ip != lastPercent){
                listener.onProgress(percent);
                lastPercent = ip;
            }
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) > -1){
            out.write(buffer, 0, len);
            savedSize += len;
            updateProgress();
        }
        in.close();
    }
}