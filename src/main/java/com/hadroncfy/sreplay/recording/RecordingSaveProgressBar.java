package com.hadroncfy.sreplay.recording;

import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;
import com.hadroncfy.sreplay.recording.mcpr.ProgressBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class RecordingSaveProgressBar extends ServerBossEvent implements ProgressBar {
    private final MinecraftServer server;
    public RecordingSaveProgressBar(MinecraftServer server, String recordingName) {
        super(TextRenderer.render(SReplayMod.getFormats().saveRecordingProgressBarTitle, recordingName), 
            BossEvent.BossBarColor.GREEN, 
            BossEvent.BossBarOverlay.PROGRESS
        );
        setPercent(0);
        for (ServerPlayer player: server.getPlayerList().getPlayers()){
            addPlayer(player);
        }
        this.server = server;
    }

    @Override
    public void onStart() {
        setPercent(0);
    }

    @Override
    public void onProgress(float percentage) {
        setPercent(percentage);
    }

    @Override
    public void onDone() {
        new Timer().schedule(new TimerTask(){
            @Override
            public void run() {
                removeAllPlayers();
            }
        }, 2000);
    }
}