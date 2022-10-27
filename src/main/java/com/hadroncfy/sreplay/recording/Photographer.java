package com.hadroncfy.sreplay.recording;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.mixin.PlayerManagerAccessor;
import com.hadroncfy.sreplay.mixin.ThreadedAnvilChunkStorageAccessor;
import com.hadroncfy.sreplay.recording.param.OptionManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.Action;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.hadroncfy.sreplay.config.TextRenderer.render;

public class Photographer extends ServerPlayer implements ISizeLimitExceededListener {
    public static final String MCPR = ".mcpr";
    public static final OptionManager PARAM_MANAGER = new OptionManager(RecordingOption.class);
    private static final String RAW_SUBDIR = "raw";
    private static final GameType MODE = GameType.SPECTATOR;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final UUID NIL = new UUID(0, 0);
    private final RecordingOption rparam;
    private int reconnectCount = 0;
    private long lastTablistUpdateTime;
    private HackyClientConnection hackyClientConnection;
    private Recorder recorder;
    private final File outputDir;
    private final List<Entity> trackedPlayers = new ArrayList<>();
    private int currentWatchDistance;
    private boolean userPaused = false;

    private String recordingFileName, saveFileName;

    public Photographer(MinecraftServer server, ServerLevel world, GameProfile profile,
            ServerPlayerGameMode im, File outputDir, RecordingOption param) {
        super(server, world, profile, im);
        currentWatchDistance = server.getPlayerList().getViewDistance();
        rparam = param;
        this.outputDir = outputDir;
    }

    public Photographer(MinecraftServer server, ServerLevel world, GameProfile profile,
            ServerPlayerGameMode im, File outputDir) {
        this(server, world, profile, im, outputDir, new RecordingOption());
    }

    public static Photographer create(String name, MinecraftServer server, ResourceKey<Level> dim, Vec3 pos, File outputDir,
            RecordingOption param) {
        GameProfile profile = new GameProfile(Player.createPlayerUUID(name), name);
        ServerLevel world = server.getLevel(dim);
        ServerPlayerGameMode im = new ServerPlayerGameMode(world);
        Photographer ret = new Photographer(server, world, profile, im, outputDir, param);
        ret.setPos(pos.x, pos.y, pos.z);
        ((PlayerManagerAccessor) server.getPlayerList()).getPlayerIo().save(ret);
        return ret;
    }

    public static Photographer create(String name, MinecraftServer server, ResourceKey<Level> dim, Vec3 pos,
            File outputDir) {
        return create(name, server, dim, pos, outputDir, RecordingOption
                .createDefaultRecordingParam(SReplayMod.getConfig(), server.getPlayerList().getViewDistance()));
    }

    private boolean checkForRecordingFileDupe(String name) {
        for (Photographer p : listFakes(getServer())) {
            if (p.saveFileName.equals(name) && p.outputDir.equals(outputDir)) {
                return true;
            }
        }
        return false;
    }

    private String genRecordingFileName(String name) {
        if (!checkForRecordingFileDupe(name)) {
            return name;
        } else {
            int i = 0;
            while (checkForRecordingFileDupe(name + "_" + i++))
                ;
            return name + "_" + i;
        }
    }

    public String getSaveName() {
        return reconnectCount == 0 ? saveFileName : saveFileName + "_" + reconnectCount;
    }

    public void setSaveName(String name) {
        reconnectCount = 0;
        saveFileName = name;
    }

    private void setWatchDistance(int distance) {
        recorder.onPacket(new ClientboundSetChunkCacheRadiusPacket(distance));
        this.reloadChunks(currentWatchDistance, rparam.watchDistance);
        currentWatchDistance = rparam.watchDistance;
    }

    private static int getChebyshevDistance(ChunkPos pos, int x, int z) {
        int i = pos.x - x;
        int j = pos.z - z;
        return Math.max(Math.abs(i), Math.abs(j));
    }

    private void reloadChunks(int oldDistance, int newDistance) {
        ServerChunkCache chunkManager = getLevel().getChunkSource();
        ThreadedAnvilChunkStorageAccessor acc = (ThreadedAnvilChunkStorageAccessor) chunkManager.chunkMap;
        SectionPos pos = this.getLastSectionPos();
        int x0 = pos.x();
        int z0 = pos.z();
        int r = oldDistance > newDistance ? oldDistance : newDistance;
        for (int x = x0 - r; x <= x0 + r; x++) {
            for (int z = z0 - r; z <= z0 + r; z++) {
                ChunkPos pos1 = new ChunkPos(x, z);
                int d = getChebyshevDistance(pos1, x0, z0);
                acc.sendWatchPackets2(this, pos1, new Packet[2], d <= oldDistance && d > newDistance,
                        d > oldDistance && d <= newDistance);
            }
        }
    }

    public int getCurrentWatchDistance() {
        return currentWatchDistance;
    }

    private void connect() throws IOException {
        recordingFileName = genRecordingFileName(getSaveName());

        final File raw = new File(outputDir, RAW_SUBDIR);
        if (!raw.exists()) {
            raw.mkdirs();
        }
        recorder = new Recorder(getGameProfile(), server, this::getWeather, new File(raw, recordingFileName), rparam);
        hackyClientConnection = new HackyClientConnection(PacketFlow.CLIENTBOUND, recorder);

        recorder.setOnSizeLimitExceededListener(this);
        recorder.start();
        lastTablistUpdateTime = System.currentTimeMillis();

        userPaused = false;

        setHealth(20.0F);
        removed = false;
        trackedPlayers.clear();
        server.getPlayerList().placeNewPlayer(hackyClientConnection, this);
        syncParams();
        gameMode.setGameModeForPlayer(MODE);// XXX: is this correct?
        getLevel().getChunkSource().move(this);

        int d = this.server.getPlayerList().getViewDistance();
        if (d != this.rparam.watchDistance) {
            recorder.onPacket(new ClientboundSetChunkCacheRadiusPacket(this.rparam.watchDistance));
            this.reloadChunks(d, this.rparam.watchDistance);
            this.currentWatchDistance = this.rparam.watchDistance;
        }
    }

    public void syncParams() {
        if (!recorder.isStopped()) {
            recorder.syncParam();
            updatePause();
            if (currentWatchDistance != rparam.watchDistance) {
                setWatchDistance(rparam.watchDistance);
            }
        }
    }

    public void connect(String saveName) throws IOException {
        reconnectCount = 0;
        saveFileName = saveName;
        connect();
    }

    @Override
    public void tick() {
        if (getServer().getTickCount() % 10 == 0) {
            connection.resetPosition();
            getLevel().getChunkSource().move(this);
        }
        super.tick();
        super.doTick();

        final long now = System.currentTimeMillis();
        if (!recorder.isStopped() && now - lastTablistUpdateTime >= 1000) {
            lastTablistUpdateTime = now;
            if (!recorder.isSoftPaused()) {
                server.getPlayerList().broadcastAll(new ClientboundPlayerInfoPacket(Action.UPDATE_DISPLAY_NAME, this));
            }
        }
    }

    private static boolean isRealPlayer(Entity entity) {
        return entity.getClass() == ServerPlayer.class;
    }

    @Override
    public void cancelRemoveEntity(Entity entity) {
        super.cancelRemoveEntity(entity);
        if (isRealPlayer(entity)) {
            trackedPlayers.add(entity);
            updatePause();
        }
    }

    @Override
    public void sendRemoveEntity(Entity entity) {
        super.sendRemoveEntity(entity);
        if (isRealPlayer(entity)) {
            trackedPlayers.remove(entity);
            updatePause();
        }
    }

    private void updatePause() {
        if (!recorder.isStopped()) {
            if (userPaused) {
                recorder.pauseRecording();
            } else if (rparam.autoPause) {
                final String name = getGameProfile().getName();
                if (trackedPlayers.isEmpty() && !recorder.isRecordingPaused()) {
                    recorder.pauseRecording();
                    server.getPlayerList().broadcastMessage(render(SReplayMod.getFormats().autoPaused, name), ChatType.CHAT, NIL);
                }
                if (!trackedPlayers.isEmpty() && recorder.isRecordingPaused()) {
                    recorder.resumeRecording();
                    server.getPlayerList().broadcastMessage(render(SReplayMod.getFormats().autoResumed, name), ChatType.CHAT, NIL);
                }
            } else {
                recorder.resumeRecording();
            }
        }
    }

    public void setPaused(boolean paused) {
        userPaused = paused;
        updatePause();
    }

    private static String timeToString(long ms) {
        final long sec = ms % 60;
        ms /= 60;
        final long min = ms % 60;
        ms /= 60;
        final long hour = ms;
        if (hour == 0) {
            return String.format("%d:%02d", min, sec);
        } else {
            return String.format("%d:%02d:%02d", hour, min, sec);
        }
    }

    @Override
    public Component getTabListDisplayName() {
        if (recorder.isStopped()) {
            return null;
        }
        long duration = recorder.getRecordedTime() / 1000;

        String time = timeToString(duration);
        if (rparam.timeLimit != -1) {
            time += "/" + timeToString(rparam.timeLimit);
        }

        String size = String.format("%.2f", recorder.getRecordedBytes() / 1024F / 1024F) + "M";
        if (rparam.sizeLimit != -1) {
            size += "/" + rparam.sizeLimit + "M";
        }
        MutableComponent ret = new TextComponent(getGameProfile().getName())
                .setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.AQUA));

        ret.append(new TextComponent(" " + time).setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GREEN)))
                .append(new TextComponent(" " + size).setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GREEN)));
        return ret;
    }

    public void tp(ResourceKey<Level> dim, double x, double y, double z) {
        if (!this.getLevel().dimension().equals(dim)) {
            ServerLevel oldMonde = server.getLevel(this.getLevel().dimension()), nouveau = server.getLevel(dim);
            oldMonde.removePlayerImmediately(this);
            removed = false;
            setLevel(nouveau);
            server.getPlayerList().sendLevelInfo(this, nouveau);
            gameMode.setLevel(nouveau);
            connection.send(new ClientboundRespawnPacket(nouveau.dimensionType(), dim, BiomeManager.obfuscateSeed(nouveau.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), nouveau.isDebug(), nouveau.isFlat(), true));
            nouveau.addDuringPortalTeleport(this);
        }
        teleportTo(x, y, z);
    }

    public Recorder getRecorder() {
        return recorder;
    }

    @Override
    public void kill() {
        kill(true);
    }

    private void postKill() {
        if (connection != null) {
            connection.onDisconnect(new TextComponent("Killed"));
        }
    }

    public CompletableFuture<Void> kill(boolean async) {
        recorder.stop();
        if (!recorder.hasSaved()) {
            final File saveFile = new File(outputDir, getSaveName() + MCPR);
            CompletableFuture<Void> f = recorder
                    .saveRecording(saveFile, new RecordingSaveProgressBar(server, saveFile.getName())).thenRun(() -> {
                        server.getPlayerList()
                                .broadcastMessage(TextRenderer.render(SReplayMod.getFormats().savedRecordingFile,
                                        getGameProfile().getName(), saveFile.getName()), ChatType.CHAT, NIL);
                    }).exceptionally(exception -> {
                        exception.printStackTrace();
                        server.getPlayerList().broadcastMessage(
                                TextRenderer.render(SReplayMod.getFormats().failedToSaveRecordingFile,
                                        getGameProfile().getName(), exception.toString()),
                                ChatType.CHAT, NIL);
                        return null;
                    });
            if (!async) {
                f.join();
            }
        }
        return CompletableFuture.runAsync(this::postKill, async ? this::executeServerTask : this::executeNow);
    }

    @Override
    public boolean hasDisconnected() {
        return false;
    }

    public void onSoftPause() {
        if (!recorder.isStopped()) {
            recorder.setSoftPaused();
        }
    }

    public void reconnect() {
        kill(true).thenRun(() -> {
            reconnectCount++;
            try {
                connect();
            } catch (IOException e) {
                server.getPlayerList().broadcastMessage(
                        TextRenderer.render(SReplayMod.getFormats().failedToStartRecording, getGameProfile().getName()),
                        ChatType.CHAT, NIL);
                e.printStackTrace();
            }
        });
    }

    private void executeServerTask(Runnable r) {
        server.tell(new TickTask(server.getTickCount(), r));
    }

    private void executeNow(Runnable r) {
        r.run();
    }

    @Override
    public void onSizeLimitExceeded(long size) {
        if (rparam.autoReconnect) {
            executeServerTask(this::reconnect);
        } else {
            executeServerTask(this::kill);
        }
    }

    public RecordingOption getRecordingParam() {
        return rparam;
    }

    public boolean isItemDisabled(){
        return this.rparam.ignoreItems;
    }

    public static void killAllFakes(MinecraftServer server, boolean async) {
        listFakes(server).forEach(p -> p.kill(async));
    }

    public static Collection<Photographer> listFakes(MinecraftServer server) {
        Collection<Photographer> ret = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player instanceof Photographer) {
                ret.add((Photographer) player);
            }
        }
        return ret;
    }

    public static Photographer getFake(MinecraftServer server, String name) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player instanceof Photographer) {
            return (Photographer) player;
        }
        return null;
    }

    public static boolean checkForSaveFileDupe(MinecraftServer server, File outputDir, String name) {
        if (new File(outputDir, name + MCPR).exists()) {
            return true;
        }
        for (Photographer p : listFakes(server)) {
            if (p.outputDir.equals(outputDir) && p.getSaveName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static int getRealViewDistance(ServerPlayer player, int watchDistance) {
        if (player instanceof Photographer) {
            return ((Photographer) player).currentWatchDistance;
        } else {
            return watchDistance;
        }
    }

    private ForcedWeather getWeather() {
        if (level.isThundering()) {
            return ForcedWeather.THUNDER;
        } else if (level.isRaining()) {
            return ForcedWeather.RAIN;
        }
        return ForcedWeather.CLEAR;
    }
}