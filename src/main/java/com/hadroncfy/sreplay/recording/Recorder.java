package com.hadroncfy.sreplay.recording;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.mixin.GameStateChangeS2CPacketAccessor;
import com.hadroncfy.sreplay.mixin.PlayerSpawnS2CPacketAccessor;
import com.hadroncfy.sreplay.mixin.WorldTimeUpdateS2CPacketAccessor;
import com.hadroncfy.sreplay.recording.mcpr.IReplayFile;
import com.hadroncfy.sreplay.recording.mcpr.Marker;
import com.hadroncfy.sreplay.recording.mcpr.Metadata;
import com.hadroncfy.sreplay.recording.mcpr.ProgressBar;
import com.hadroncfy.sreplay.recording.mcpr.SReplayFile;
import com.mojang.authlib.GameProfile;

import net.minecraft.SharedConstants;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Recorder implements IPacketListener {
    private static final String MARKER_PAUSE = "PAUSE";
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftServer server;
    private final GameProfile profile;
    private final WeatherView wv;
    
    private final IReplayFile replayFile;
    private final Metadata metaData;
    private final RecordingOption param;
    
    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private long startTime;
    private int startTick;
    private long lastPacket;
    private long timeShift = 0;
    private ConnectionProtocol nstate = ConnectionProtocol.LOGIN;
    private boolean stopped = false;
    private boolean isSaving = false;
    private boolean hasSaved0 = false;

    private boolean paused = false;
    private boolean resumeOnNextPacket = true;
    private boolean followTick = false;
    private final List<Marker> markers = new ArrayList<>();

    private ISizeLimitExceededListener limiter = null;

    public Recorder(GameProfile profile, MinecraftServer server, WeatherView wv, File outputPath, RecordingOption param) throws IOException {
        this.server = server;
        this.profile = profile;
        this.wv = wv;

        this.param = param;

        replayFile = new SReplayFile(outputPath);
        metaData = new Metadata();
    }

    public void setOnSizeLimitExceededListener(ISizeLimitExceededListener l){
        limiter = l;
    }

    public long getStartTime(){
        return startTime;
    }

    public void start() {
        startTime = System.currentTimeMillis();
        startTick = server.getTickCount();
        
        metaData.singleplayer = false;
        metaData.serverName = SReplayMod.getConfig().serverName;
        metaData.generator = "sreplay";
        metaData.date = startTime;
        metaData.mcversion = SharedConstants.getCurrentVersion().getName();
        server.getPlayerList()
            .broadcastMessage(TextRenderer.render(SReplayMod.getFormats().startedRecording, profile.getName()), ChatType.CHAT, new UUID(0, 0));

        // Must contain this packet, otherwise ReplayMod would complain
        savePacket(new ClientboundGameProfilePacket(profile));
        nstate = ConnectionProtocol.PLAY;
    }

    public void setSoftPaused(){
        paused = true;
    }

    // Should only increase
    public long getRecordedTime(){
        final long base;
        if (followTick){
            base = (server.getTickCount() - startTick) * 50L;
        }
        else {
            base = System.currentTimeMillis() - startTime;
        }
        return base - timeShift;
    }

    private synchronized long getCurrentTimeAndUpdate(){
        long now = getRecordedTime();
        if (paused){
            if (resumeOnNextPacket){
                paused = false;
            }
            timeShift += now - lastPacket;
            return lastPacket;
        }
        return lastPacket = now;
    }

    public void pauseRecording(){
        resumeOnNextPacket = false;
        paused = true;
        if (param.pauseMarkers){
            addMarker(MARKER_PAUSE);
        }
    }

    public boolean isRecordingPaused(){
        return !resumeOnNextPacket;
    }

    public boolean isSoftPaused(){
        return resumeOnNextPacket && paused;
    }

    public void resumeRecording(){
        resumeOnNextPacket = true;
    }

    public synchronized void setFollowTick(boolean f){
        if (followTick != f){
            final long t1 = getRecordedTime();
            followTick = f;
            final long t2 = getRecordedTime();
            timeShift = t2 - t1;
        }
        else {
            followTick = f;
        }
    }

    private void savePacket(Packet<?> packet) {
        long bytesRecorded = replayFile.getRecordedBytes();
        if (param.sizeLimit != -1 && bytesRecorded > ((long)param.sizeLimit) << 20 || param.timeLimit != -1 && getRecordedTime() > (long)param.timeLimit * 1000){
            stop();
            if (limiter != null){
                limiter.onSizeLimitExceeded(bytesRecorded);
            }
            return;
        }
        try {
            final long timestamp = getCurrentTimeAndUpdate();
            final boolean login = nstate == ConnectionProtocol.LOGIN;
            saveService.submit(() -> {
                try {
                    replayFile.savePacket(timestamp, packet, login);
                } catch (Exception e) {
                    LOGGER.error("Error saving packet", e);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error saving packet", e);
            e.printStackTrace();
        }
    }

    public void stop(){
        stopped = true;
    }

    public boolean isStopped(){
        return stopped;
    }

    public long getRecordedBytes(){
        return replayFile.getRecordedBytes();
    }

    public void addMarker(String name){
        Marker m = new Marker(name, (int) getRecordedTime());
        markers.add(m);

        saveMarkers();
    }

    public List<Marker> getMarkers(){
        return markers;
    }

    public void removeMarker(int i){
        markers.remove(i);
        saveMarkers();
    }

    private void saveMetadata(){
        saveService.submit(() -> {
            try {
                replayFile.saveMetaData(metaData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveMarkers(){
        if (!markers.isEmpty()){
            saveService.submit(() -> {
                try {
                    replayFile.saveMarkers(markers);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public boolean hasSaved(){
        return hasSaved0;
    }

    public CompletableFuture<Void> saveRecording(File dest, ProgressBar bar) {
        hasSaved0 = true;
        if (!isSaving){
            isSaving = true;
            metaData.duration = (int) lastPacket;
            server.getPlayerList().broadcastMessage(TextRenderer.render(SReplayMod.getFormats().savingRecordingFile, profile.getName()), ChatType.CHAT, new UUID(0, 0));
            return CompletableFuture.runAsync(() -> {
                saveMetadata();
                saveMarkers();
                saveService.shutdown();
                boolean interrupted = false;
                try {
                    saveService.awaitTermination(10, TimeUnit.SECONDS);
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
                try {
                    replayFile.closeAndSave(dest, bar);
                }
                catch(IOException e){
                    e.printStackTrace();
                    throw new CompletionException(e);
                }
                finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, r -> {
                final Thread t = new Thread(r, "Recording file save thread");
                t.start();
            });
        }
        else {
            LOGGER.warn("saveRecording() called twice");
            return CompletableFuture.supplyAsync(() -> {
                throw new IllegalStateException("saveRecording() called twice");
            });
        }
    }

    private void setWeather(ForcedWeather weather){
        switch(weather){
            case RAIN:
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1));
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0));
                break;
            case CLEAR:
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0));
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0));
                break;
            case THUNDER:
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1));
                savePacket(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1));
                break;
            default:
                break;
        }
    }

    public void syncParam(){
        switch(param.forcedWeather){
            case RAIN:
            case CLEAR:
            case THUNDER:
                setWeather(param.forcedWeather);
                break;
            case NONE:
                setWeather(wv.getWeather());
                break;
        }
    }

    @Override
    public void onPacket(Packet<?> p) {
        if (!stopped){
            if (p instanceof ClientboundAddPlayerPacket){
                metaData.players.add(((PlayerSpawnS2CPacketAccessor) p).getPlayerId());
                saveMetadata();
            }
            if (p instanceof ClientboundDisconnectPacket){
                return;
            }
            if (param.dayTime != -1 && p instanceof ClientboundSetTimePacket){
                final WorldTimeUpdateS2CPacketAccessor p2 = (WorldTimeUpdateS2CPacketAccessor)p;
                p = new ClientboundSetTimePacket(p2.getGameTime(), param.dayTime, false);
            }
            if (param.forcedWeather != ForcedWeather.NONE && p instanceof ClientboundGameEventPacket){
                ClientboundGameEventPacket.Type r = ((GameStateChangeS2CPacketAccessor)p).getEvent();
                if (
                    r == ClientboundGameEventPacket.START_RAINING
                    || r == ClientboundGameEventPacket.STOP_RAINING
                    || r == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
                    || r == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                ) return;
            }
            if (param.ignoreChat && p instanceof ClientboundChatPacket){
                return;
            }
            savePacket(p);
        }
    }
}