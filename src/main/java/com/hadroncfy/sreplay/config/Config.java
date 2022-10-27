package com.hadroncfy.sreplay.config;

import java.io.File;
import java.net.InetAddress;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
        .registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
        .registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory())
        .registerTypeAdapter(Pattern.class, new PatternSerializer())
        .registerTypeAdapter(File.class, new FileSerializer()).create();

    public File savePath = new File("replay_recordings");
    public String serverName = "localhost";
    public InetAddress serverListenAddress = InetAddress.getLoopbackAddress();
    public String serverHostName = "localhost";
    public int serverPort = 12346;
    public long downloadTimeout = 60000;
    public int sizeLimit = -1;
    public int timeLimit = -1;
    public int itemsPerPage = 20;
    public boolean autoReconnect = true;
    public boolean debugCrash = false;
    public Pattern playerNamePattern = Pattern.compile("^cam_.*$");

    public Formats formats = new Formats();
}