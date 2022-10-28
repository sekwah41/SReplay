package com.hadroncfy.sreplay.command;

import com.google.gson.JsonParseException;
import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.recording.Photographer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.literal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;
import static com.hadroncfy.sreplay.recording.Photographer.MCPR;
import static com.hadroncfy.sreplay.config.TextRenderer.render;
import static com.hadroncfy.sreplay.SReplayMod.getConfig;
import static com.hadroncfy.sreplay.SReplayMod.getFormats;
import static com.hadroncfy.sreplay.Util.tryGetArg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelFuture;

public class SReplayCommand {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private static final ConfirmationManager cm = new ConfirmationManager(20000, 999);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        final LiteralArgumentBuilder<CommandSourceStack> b = literal("sreplay")
            .then(literal("player").then(argument("player", StringArgumentType.word())
                .suggests((src, sb) -> suggest(Photographer.listFakes(src.getSource().getServer()).stream().map(p -> p.getGameProfile().getName()), sb))
                .then(literal("spawn").executes(SReplayCommand::playerSpawn))
                .then(literal("kill").executes(SReplayCommand::playerKill))
                .then(literal("respawn").executes(SReplayCommand::playerRespawn))
                .then(literal("tp").executes(SReplayCommand::playerTp))
                .then(literal("name").executes(SReplayCommand::getName)
                    .then(argument("fileName", StringArgumentType.greedyString()).executes(SReplayCommand::setName)))
                .then(Photographer.PARAM_MANAGER.buildCommand())
                .then(literal("pause").executes(SReplayCommand::pause))
                .then(literal("resume").executes(SReplayCommand::resume))
                .then(literal("locate").executes(SReplayCommand::locate))
                .then(literal("marker")
                    .then(literal("list").executes(SReplayCommand::getMarkers)
                        .then(argument("page", IntegerArgumentType.integer(1)).executes(SReplayCommand::getMarkers)))
                    .then(literal("add").then(argument("marker", StringArgumentType.greedyString()).executes(SReplayCommand::marker)))
                    .then(literal("remove").then(argument("markerId", IntegerArgumentType.integer(1)).executes(SReplayCommand::removeMarker))))))
            .then(literal("list").executes(SReplayCommand::listRecordings)
                .then(argument("page", IntegerArgumentType.integer(1)).executes(SReplayCommand::listRecordings)))
            .then(literal("delete").then(argument("recording", StringArgumentType.greedyString())
                .suggests(SReplayCommand::suggestRecordingFile)
                .executes(SReplayCommand::deleteRecording)))
            .then(literal("confirm")
                .then(argument("code", IntegerArgumentType.integer(0)).executes(SReplayCommand::confirm)))
            .then(literal("cancel").executes(SReplayCommand::cancel))
            .then(literal("reload").executes(SReplayCommand::reload))
            .then(literal("server")
                .then(literal("start").executes(SReplayCommand::startServer))
                .then(literal("stop").executes(SReplayCommand::stopServer)))
            .then(literal("get")
                .then(argument("fileName", StringArgumentType.greedyString())
                .suggests(SReplayCommand::suggestRecordingFile)
                .executes(SReplayCommand::getFile)))
            .then(literal("help").executes(SReplayCommand::help)
                .then(Photographer.PARAM_MANAGER.buildHelpCommand()))
            .then(literal("crash")
                .requires(src -> getConfig().debugCrash)
                .executes(SReplayCommand::simulateCrash));
        d.register(b);
    }

    private static CompletableFuture<Suggestions> suggestRecordingFile(CommandContext<CommandSourceStack> src, SuggestionsBuilder sb){
        return suggest(SReplayMod.listRecordings().stream().map(File::getName), sb);
    }

    private static int simulateCrash(CommandContext<CommandSourceStack> ctx){
        throw new Error("manually triggered crash");
    }

    private static int help(CommandContext<CommandSourceStack> ctx){
        for (Component t: SReplayMod.getFormats().help){
            ctx.getSource().sendSuccess(t, false);
        }
        return 0;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            ctx.getSource().sendSuccess(render(getFormats().botLocation,
                p.getGameProfile().getName(),
                String.format("%.0f", p.getX()),
                String.format("%.0f", p.getY()),
                String.format("%.0f", p.getZ()),
                p.getLevel().dimension().location().getPath()
            ), false);
        }
        return 0;
    }

    private static <T> void paginate(CommandContext<CommandSourceStack> ctx, List<T> p, BiConsumer<Integer, T> consumer){
        CommandSourceStack src = ctx.getSource();
        int page = tryGetArg(() -> IntegerArgumentType.getInteger(ctx, "page"), () -> 1) - 1;
        int s = getConfig().itemsPerPage;
        int start = page * s, end = start + s;
        if (start >= p.size() || start < 0 || end < 0){
            ctx.getSource().sendFailure(getFormats().invalidPageNum);
            return;
        }
        if (end >= p.size()){
            end = p.size() - 1;
        }
        int i = start;
        for (T v: p.subList(start, end)){
            consumer.accept(i++, v);
        }

        src.sendSuccess(render(getFormats().paginationFooter, page + 1, (int)Math.ceil(p.size() / (float)s)), false);
    }

    private static int getMarkers(CommandContext<CommandSourceStack> ctx){
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            final String name = p.getGameProfile().getName();
            final CommandSourceStack src = ctx.getSource();

            src.sendSuccess(render(SReplayMod.getFormats().markerListTitle, name), false);
            paginate(ctx, p.getRecorder().getMarkers(), (i, marker) -> {
                src.sendSuccess(render(SReplayMod.getFormats().markerListItem, name, Integer.toString(i), marker.name), false);
            });
        }
        return 0;
    }

    private static int removeMarker(CommandContext<CommandSourceStack> ctx){
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            final String name = p.getGameProfile().getName();
            final CommandSourceStack src = ctx.getSource();
            final int id = IntegerArgumentType.getInteger(ctx, "markerId") - 1;
            if (id < 0 || id >= p.getRecorder().getMarkers().size()){
                src.sendFailure(SReplayMod.getFormats().invalidMarkerId);
                return 1;
            }
            p.getRecorder().removeMarker(id);
            src.getServer().getPlayerList().broadcastSystemMessage(render(SReplayMod.getFormats().markerRemoved, ctx.getSource().getTextName(), name, Integer.toString(id + 1)), true);
        }
        return 0;
    }

    private static int getFile(CommandContext<CommandSourceStack> ctx){
        final String fName = StringArgumentType.getString(ctx, "fileName");
        final File f = new File(SReplayMod.getConfig().savePath, fName);
        if (f.exists()){
            final String path = SReplayMod.getServer().addFile(f, SReplayMod.getConfig().downloadTimeout);
            String url = "http://" + SReplayMod.getConfig().serverHostName;
            final int port = SReplayMod.getConfig().serverPort;
            if (port != 80){
                url += ":" + port;
            }
            url += path;
            ctx.getSource().sendSuccess(render(SReplayMod.getFormats().downloadUrl, url), false);
        }
        else {
            ctx.getSource().sendFailure(render(SReplayMod.getFormats().fileNotFound, fName));
            return 1;
        }
        return 0;
    }

    private static int startServer(CommandContext<CommandSourceStack> ctx){
        final CommandSourceStack src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        try {
            final ChannelFuture ch = SReplayMod.getServer().bind(SReplayMod.getConfig().serverListenAddress, SReplayMod.getConfig().serverPort);
            ch.addListener(future -> {
                if (future.isSuccess()){
                    server.getPlayerList().broadcastSystemMessage(SReplayMod.getFormats().serverStarted, true);
                }
                else {
                    src.sendFailure(render(SReplayMod.getFormats().serverStartFailed, future.cause().getMessage()));
                }
            });
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    private static int stopServer(CommandContext<CommandSourceStack> ctx){
        final CommandSourceStack src = ctx.getSource();
        final MinecraftServer server = src.getServer();
        final ChannelFuture ch = SReplayMod.getServer().stop();
        ch.addListener(future -> {
            if (future.isSuccess()){
                server.getPlayerList().broadcastSystemMessage(SReplayMod.getFormats().serverStopped, true);
            }
            else {
                src.sendFailure(render(SReplayMod.getFormats().serverStopFailed, future.cause().getMessage()));
            }
        });
        return 0;
    }

    private static int getName(CommandContext<CommandSourceStack> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            ctx.getSource().sendSuccess(render(SReplayMod.getFormats().recordingFile, p.getGameProfile().getName(), p.getSaveName()), false);
            return 1;
        }
        return 0;
    }

    private static int setName(CommandContext<CommandSourceStack> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            String name = StringArgumentType.getString(ctx, "fileName");
            if (name.endsWith(MCPR)){
                name = name.substring(0, name.length() - MCPR.length());
            }
            if (Photographer.checkForSaveFileDupe(ctx.getSource().getServer(), SReplayMod.getConfig().savePath, name)){
                ctx.getSource().sendFailure(render(SReplayMod.getFormats().recordFileExists, name));
                return 0;
            }
            p.setSaveName(name);
            ctx.getSource().sendSuccess(render(SReplayMod.getFormats().renamedFile, ctx.getSource().getTextName(), p.getGameProfile().getName(), name), true);
            return 1;
        }
        return 0;
    }

    static Photographer requirePlayer(CommandContext<CommandSourceStack> ctx){
        String name = StringArgumentType.getString(ctx, "player");
        Photographer p = SReplayMod.getFake(ctx.getSource().getServer(), name);
        if (p != null){
            return p;
        }
        else {
            try {
                ctx.getSource().sendSuccess(render(SReplayMod.getConfig().formats.playerNotFound, name), true);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }

    public static int marker(CommandContext<CommandSourceStack> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            String name = StringArgumentType.getString(ctx, "marker");
            p.getRecorder().addMarker(name);
            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(render(SReplayMod.getFormats().markerAdded, ctx.getSource().getTextName(), p.getGameProfile().getName(), name), true);
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int pause(CommandContext<CommandSourceStack> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            p.setPaused(true);
            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(render(SReplayMod.getFormats().recordingPaused, ctx.getSource().getTextName(), p.getGameProfile().getName()), true);
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int resume(CommandContext<CommandSourceStack> ctx){
        Photographer p = requirePlayer(ctx);
        if (p != null){
            p.setPaused(false);
            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(render(SReplayMod.getFormats().recordingResumed, ctx.getSource().getTextName(), p.getGameProfile().getName()), true);
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int reload(CommandContext<CommandSourceStack> ctx){
        try {
            SReplayMod.loadConfig();
            ctx.getSource().sendSuccess(render(SReplayMod.getFormats().reloadedConfig), false);
            return 1;
        } catch (IOException | JsonParseException e) {
            e.printStackTrace();
            ctx.getSource().sendSuccess(render(SReplayMod.getFormats().failedToReloadConfig, e.toString()), false);
            return 0;
        }
    }

    public static int confirm(CommandContext<CommandSourceStack> ctx) {
        final int code = IntegerArgumentType.getInteger(ctx, "code");
        if (!cm.confirm(ctx.getSource().getTextName(), code)) {
            ctx.getSource().sendSuccess(SReplayMod.getFormats().nothingToConfirm, false);
        }

        return 0;
    }

    public static int cancel(CommandContext<CommandSourceStack> ctx) {
        if (!cm.cancel(ctx.getSource().getTextName())) {
            ctx.getSource().sendSuccess(SReplayMod.getFormats().nothingToCancel, false);
        }
        return 0;
    }

    public static int listRecordings(CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack src = ctx.getSource();
        src.sendSuccess(SReplayMod.getFormats().recordingFileListHead, false);

        paginate(ctx, SReplayMod.listRecordings(), (i, f) -> {
            String size = String.format("%.2f", f.length() / 1024F / 1024F);
            src.sendSuccess(render(SReplayMod.getFormats().recordingFileItem, f.getName(), size), false);
        });
        
        return 0;
    }

    public static int deleteRecording(CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack src = ctx.getSource();
        final File rec = new File(SReplayMod.getConfig().savePath, StringArgumentType.getString(ctx, "recording"));
        final MinecraftServer server = src.getServer();
        if (rec.exists()) {
            src.sendSuccess(render(SReplayMod.getFormats().aboutToDeleteRecording, rec.getName()), true);
            cm.submit(src.getTextName(), src, () -> {
                try {
                    Files.delete(rec.toPath());
                    server.getPlayerList()
                        .broadcastSystemMessage(render(SReplayMod.getFormats().deletedRecordingFile, src.getTextName(), rec.getName()), true);
                } catch (IOException e) {
                    e.printStackTrace();
                    server.getPlayerList().broadcastSystemMessage(render(
                        SReplayMod.getFormats().failedToDeleteRecordingFile,
                        src.getTextName(),
                        rec.getName(),
                        e
                    ), true);
                }
            });
        } else {
            src.sendSuccess(render(SReplayMod.getFormats().fileNotFound, rec.getName()), true);
        }
        return 0;
    }

    public static UUID getSenderUUID(CommandContext<CommandSourceStack> ctx){
        try {
            return ctx.getSource().getPlayerOrException().getUUID();
        }
        catch(CommandSyntaxException e){
            return UUID.randomUUID();
        }
    }

    public static int playerTp(CommandContext<CommandSourceStack> ctx) {
        final Photographer p = requirePlayer(ctx);
        if (p != null){
            Vec3 pos = ctx.getSource().getPosition();
            p.tp(ctx.getSource().getLevel().dimension(), pos.x, pos.y, pos.z);
            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(render(SReplayMod.getFormats().teleportedBotToYou, p.getGameProfile().getName(), ctx.getSource().getTextName()), true);
            LOGGER.info("Teleported {} to {}", p.getGameProfile().getName(), ctx.getSource().getTextName());
            return 1;
        }

        return 0;
    }

    public static int playerSpawn(CommandContext<CommandSourceStack> ctx) {
        final String pName = StringArgumentType.getString(ctx, "player");
        final CommandSourceStack src = ctx.getSource();
        final MinecraftServer server = src.getServer();

        Matcher m = SReplayMod.getConfig().playerNamePattern.matcher(pName);
        if (!m.matches()) {
            src.sendSuccess(SReplayMod.getFormats().invalidPlayerName, true);
            return 0;
        }
        if (pName.length() > 16) {
            src.sendSuccess(SReplayMod.getFormats().playerNameTooLong, true);
            return 0;
        }
        if (server.getPlayerList().getPlayerByName(pName) != null) {
            src.sendSuccess(render(SReplayMod.getFormats().playerIsLoggedIn, pName), true);
            return 0;
        }

        String saveName = sdf.format(new Date());
        int i = 0;
        // Although this never happen in normal situations, we'll take care of it anyway
        if (Photographer.checkForSaveFileDupe(server, SReplayMod.getConfig().savePath, saveName)){
            while (Photographer.checkForSaveFileDupe(server, SReplayMod.getConfig().savePath, saveName + "_" + i++));
            saveName = saveName + "_" + i++;
        }

        try {
            Photographer photographer = Photographer.create(pName, server, src.getLevel().dimension(), src.getPosition(), SReplayMod.getConfig().savePath);
            photographer.connect(saveName);
        } catch (IOException e) {
            src.sendSuccess(render(SReplayMod.getFormats().failedToStartRecording, e.toString()), false);
            e.printStackTrace();
        }
        return 1;
    }

    public static int playerKill(CommandContext<CommandSourceStack> ctx) {
        final Photographer p = requirePlayer(ctx);
        CommandSourceStack src = ctx.getSource();
        if (p != null){
            src.sendSuccess(render(getFormats().killConfirm, p.getGameProfile().getName()), false);
            cm.submit(src.getTextName(), src, p::kill);
            return 1;
        }
        return 0;
    }

    public static int playerRespawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final Photographer p = requirePlayer(ctx);
        CommandSourceStack src = ctx.getSource();
        if (p != null) {
            src.sendSuccess(render(getFormats().killConfirm, p.getGameProfile().getName()), false);
            cm.submit(src.getTextName(), src, p::reconnect);
            return 1;
        }
        return 0;
    }
}