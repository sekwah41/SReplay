package com.hadroncfy.sreplay.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class Formats {
    private static MutableComponent red(String s){
        return Component.literal(s).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
    }
    private static MutableComponent green(String s){
        return Component.literal(s).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
    }
    private static MutableComponent white(String s){
        return Component.literal(s).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
    }
    private static MutableComponent yellow(String s){
        return Component.literal(s).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
    }

    public Component playerNotFound = red("[SReplay] Player %1$s not found (or not a video bot)"),
    recordFileExists = red("[SReplay] Video file %1$s already exists"),
    reloadedConfig = Component.literal("[SReplay] Configuration loaded"),
    failedToReloadConfig = red("[SReplay] Failed to load configuration: %1$s"),
    nothingToConfirm = red("[SReplay] No pending actions\n"),
    nothingToCancel = red("[SReplay] No operations to cancel"),
    confirmingHint = Component.literal("[SReplay] use")
        .append(Component.literal("/sreplayer confirm %1$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append(Component.literal("to confirm this action")),
    deletedRecordingFile = Component.literal("[SReplay] %1$s: Deleted video file %2$s"),
    failedToDeleteRecordingFile = red("[SReplay] %1$s: Failed to delete video file %2$s：%3$s"),
    operationCancelled = Component.literal("[SReplay] Operation cancelled\n"),
    incorrectConfirmationCode = red("[SReplay] Confirmation code does not match"),
    fileNotFound = red("[SReplay] file %1$s does not exist"),
    teleportedBotToYou = Component.literal("[SReplay] has been")
        .append(Component.literal("%1$s").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)))
        .append(Component.literal("Send to %2$s")),
    invalidPlayerName = red("[SReplay] invalid player name\n"),
    playerNameTooLong = red("[SReplay] Player name length cannot exceed 16 (otherwise it will be chunk ban!)"),
    playerIsLoggedIn = red("[SReplay] Player %1$s is logged in\n"),
    failedToStartRecording = red("[SReplay] Recording failed\n：%1$s"),
    recordingFileListHead = Component.literal("[SReplay] Recording file list\n："),
    recordingFileItem = Component.literal("- %1$s(%2$sM) ").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
        .append(Component.literal("[download]").setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withClickEvent(
            new ClickEvent(Action.RUN_COMMAND, "/sreplay get %1$s")
        ).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            Component.literal("Click to generate download link").setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY))
        ))))
        .append(Component.literal("[delete]").setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(
            new ClickEvent(Action.RUN_COMMAND, "/sreplay delete %1$s")
        ).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            Component.literal("Click to delete").setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY))
        )))),
    savingRecordingFile = Component.literal("[SReplay] Saving")
        .append(Component.literal("%1$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
        .append(Component.literal("'s video file")),
    savedRecordingFile = Component.literal("[SReplay] Saved")
        .append(Component.literal("%1$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
        .append(Component.literal("'s video file"))
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))),
    failedToSaveRecordingFile = red("[SReplay] Failed to save video file of %1$s：%2$s"),
    startedRecording = Component.literal("[SReplay] %1$s has started recording"),
    aboutToDeleteRecording = Component.literal("[SReplay] Recording file to be deleted %1$s"),
    recordingFile = Component.literal("[SReplay] %1$s is recording")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))),
    sizeLimitTooSmall = red("[SReplay] The size limit cannot be less than 10M"),
    timeLimitTooSmall = red("[SReplay] The time limit cannot be less than 10s"),
    recordingPaused = Component.literal("[SReplay] %1$s: ")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append(Component.literal("Recording paused")),
    recordingResumed = Component.literal("[SReplay] %1$s: ")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append(Component.literal("Recording resumed")),
    markerAdded = Component.literal("[SReplay] %1$s: 已在")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append(Component.literal("add tag"))
        .append(Component.literal("%3$s").setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GREEN))),
    markerRemoved = Component.literal("[SReplay] %1$s: 已在")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append(Component.literal("delete marker"))
        .append(Component.literal("%3$s").setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GREEN))),
    invalidMarkerId = red("[SReplay] Invalid token sequence number"),
    markerListTitle = Component.literal("[SReplay] ")
        .append(Component.literal("%1$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append("all tags of\n："),
    markerListItem = Component.literal("- [%2$s] %3$s")
        .append(Component.literal("[delete]").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)
            .withClickEvent(new ClickEvent(Action.RUN_COMMAND, "/sreplay player %1$s marker remove %2$s"))
        )),
    renamedFile = Component.literal("[SReplay] %1$s: has been")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
        .append(Component.literal("The filename is set to\n"))
        .append(Component.literal("%3$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))),
    serverStarted = Component.literal("[SReplay] Download server started"),
    serverStartFailed = red("[SReplay] Download server failed to start：%1$s"),
    serverStopped = Component.literal("[SReplay] Download server stopped"),
    serverStopFailed = Component.literal("[SReplay] Download server stop failed：%1$s"),
    downloadUrl = Component.literal("[SReplay] download link：")
        .append(Component.literal("%1$s").setStyle(Style.EMPTY.applyFormat(ChatFormatting.UNDERLINE)
            .withClickEvent(new ClickEvent(Action.OPEN_URL, "%1$s"))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.literal("Click to download").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true))
            )))),
    autoPaused = Component.literal("[SReplay] %1$s: No players nearby, pause recording"),
    autoResumed = Component.literal("[SReplay] %1$s: There is a player nearby, continue recording"),
    setParam = Component.literal("[SReplay] %1$s: Will")
        .append(Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)))
        .append(Component.literal("of"))
        .append(Component.literal("%3$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)))
        .append(Component.literal("value is set to"))
        .append(Component.literal("%4$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))),
    getParam = Component.literal("[SReplay]").append(
        Component.literal("%1$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
    ).append(Component.literal("of")).append(
        Component.literal("%2$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
    ).append(Component.literal("value is")).append(
        Component.literal("%3$s").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))
    ),
    positiveParam = red("[SReplay] This parameter must be a positive integer"),
    nonNegativeOrMinusOne = red("[SReplay] This parameter must be -1 or non-negative"),
    invalidEnum = red("[SReplay] invalid value"),
    paginationFooter = Component.literal("No.").append(
        green("(%1$s/%2$s)")
    ).append(white("Page")),
    invalidPageNum = red("[SReplay] invalid page number"),
    noSuchParam = red("[SReplay] without this parameter"),
    paramHelp = yellow("%1$s: ").append(green("%2$s")),
    botLocation = white("[SReplay] ")
        .append(green("%1$s"))
        .append(white("lie in"))
        .append(white("[x: %2$s, y: %3$s, z: %4$s, dim: %5$s]")),
    saveRecordingProgressBarTitle = white("save video file").append(green("%1$s")),
    killConfirm = Component.literal("[SReplay] sure you want to stop")
        .append(green("%1$s"))
        .append(white("recording？"));

    public Component[] help = new Component[]{
        Component.literal("====== SReplay usage ======").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)),
        green("/sreplay player <player name> ..."),
        green("- spawn ").append(white("Summon a video dummy and start recording")),
        green("- kill ").append(white("Kick off the designated video dummy and save the video file")),
        green("- respawn ").append(white("Kick off the dummy first, save the file, and start a new round of recording in place")),
        green("- name [file name]").append(white("Get or set the video file name")),
        green("- tp").append(white("Teleport a video dummy to your location")),
        green("- pause").append(white("Pause recording")),
        green("- resume").append(white("continue recording")),
        green("- locate").append(white("Show dummy location")),
        green("- marker list [page number]").append(white("List all added tags")),
        green("- marker add [tagname]").append(white("Add a marker at the current location")),
        green("- marker remove [Mark serial number]").append(white("delete a marker")),
        green("- set <option name> [parameter value]").append(white("Set or get the value of the corresponding recording option. For details, please use")).append(green("/sreplay help set <option name>")),
        white(""),
        green("/sreplay list [page number]").append(white("List all recorded files")),
        green("/sreplay delete <file name>").append(white("Delete the given video file. need to use")).append(green("/sreplay confirm <confirmation code>")).append(white("来确认")),
        green("/sreplay reload").append(white("reload config file")),
        green("/sreplay server <start|stop>").append(white("Start/stop the http server for downloading video files")),
        green("/sreplay get <file name>").append(white("Download the specified video file. The command returns a temporary link to download the file."))
    };
}