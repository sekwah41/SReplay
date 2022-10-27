package com.hadroncfy.sreplay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hadroncfy.sreplay.recording.Photographer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class Util {
    // Stolen from fabric-carpet
    @FunctionalInterface
    public interface SupplierWithCommandSyntaxException<T> {
        T get() throws IllegalArgumentException;
    }

    @FunctionalInterface
    public interface Replacer<T> {
        T get(T a);
    }

    public static <T> T tryGetArg(SupplierWithCommandSyntaxException<T> a, SupplierWithCommandSyntaxException<T> b) {
        try {
            return a.get();
        }
        catch (IllegalArgumentException e) {
            return b.get();
        }
    }

    public static Component makeBroadcastMsg(String player, String msg){
        return new TextComponent("[" + player + ": " + msg + "]").setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.DARK_GRAY));
    }

    public static Collection<Photographer> getFakes(MinecraftServer server){
        Collection<Photographer> ret = new ArrayList<>();
        for (ServerPlayer player: server.getPlayerList().getPlayers()){
            if (player instanceof Photographer){
                ret.add((Photographer) player);
            }
        }
        return ret;
    }

    public static Photographer getFake(MinecraftServer server, String name){
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player != null && player instanceof Photographer){
            return (Photographer) player;
        }
        return null;
    }

    public static String replaceAll(Pattern pattern, String s, Replacer<String> func){
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        Matcher m = pattern.matcher(s);
        while (m.find()){
            if (lastIndex != m.start()){
                sb.append(s.substring(lastIndex, m.start()));
            }
            String name = m.group();
            lastIndex = m.start() + name.length();
            sb.append(func.get(name));
        }
        if (lastIndex < s.length()){
            sb.append(s.substring(lastIndex));
        }
        return sb.toString();
    }
}