package com.sunekaer.mods.sdrp.discord;

import com.sunekaer.mods.sdrp.SDRP;
import com.sunekaer.mods.sdrp.config.Config;
import com.sunekaer.mods.sdrp.discord.discordipc.IPCClient;
import com.sunekaer.mods.sdrp.discord.discordipc.entities.RichPresence;
import com.sunekaer.mods.sdrp.discord.discordipc.exceptions.NoDiscordClientException;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Dimension;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;


import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DiscordRichPresence {

    private static final OffsetDateTime TIME = OffsetDateTime.now();
    private static final Timer TIMER = new Timer("DiscordRP Timer");
    private static State currentState;
    private static final IPCClient CLIENT = new IPCClient(Config.CONFIG.clientID.get());
    private static boolean isEnabled = false;
    private static int errorCount = 0;
    private static TimerTask timerTask;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(), "DiscordRP Stop"));
    }

    public static void start() {
        try {
            CLIENT.connect();
            TIMER.schedule(timerTask = new TimerTask() {

                @Override
                public void run() {
                    setState(currentState);
                }
            }, 1000, 1000 * 120);
            isEnabled = true;
            SDRP.LOGGER.info("Discord client found and connected.");
        } catch (NoDiscordClientException ex) {
            SDRP.LOGGER.info("Discord client was not found.");
        }
    }

    public static void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        try {
            CLIENT.close();
        } catch (Exception ex) {
        }
        errorCount = 0;
        isEnabled = false;
        SDRP.LOGGER.info("Discord client closed.");
    }

    public static void setDimension(World world) {
        State dim = map.get(world.func_234923_W_().func_240901_a_().toString());
        if (dim != null) {
            setState(dim);
        } else if (dim == null){
            String name = I18n.format("sdrp." + world.func_234923_W_().func_240901_a_().getPath());
            String in = I18n.format("sdrp." + world.func_234923_W_().func_240901_a_().getPath() + ".in");
            String key = world.func_234923_W_().func_240901_a_().getPath();

            State dim2 = new State(in,  name, key);
            setState(dim2);
        }
    }

    public static void setState(State state) {
        currentState = state;
        RichPresence.Builder builder = new RichPresence.Builder();
//        builder.setDetails();
        builder.setState(state.message);
        builder.setStartTimestamp(TIME);
//        String name = new TranslationTextComponent("sdrp.logo").getFormattedText();
        String name = I18n.format("sdrp.logo");
        builder.setLargeImage("logo", name);
        builder.setSmallImage(state.imageKey, state.imageName);
        try {
            CLIENT.sendRichPresence(builder.build());
        } catch (Exception ex) {
            SDRP.LOGGER.error(ex);
            try {
                CLIENT.connect();
                errorCount = 0;
                CLIENT.sendRichPresence(builder.build());
            } catch (Exception ex2) {
                try {
                    CLIENT.close();
                } catch (Exception ex3) {
                }
                errorCount++;
                if (errorCount > 10) {
                    SDRP.LOGGER.info("DiscordRP connection failed.");
                    stop();
                }
            }
        }
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static State getCurrent() {
        return currentState;
    }

    public static class State {

        public String message;
        public String imageName;
        public String imageKey;

        public State(String m, String n, String k) {
            message = m;
            imageName = n;
            imageKey = k;
        }
    }

    public static HashMap<String, State> map = new HashMap<>();

    static {
        map.put("loading", new State("Starting Minecraft", "Starting Minecraft", "loading"));
        map.put("menu", new State("Main Menu", "Main Menu", "menu"));
//        map.put("minecraft:overworld", new State("In The Overworld", "Overworld", "overworld"));
//        map.put("minecraft:the_nether", new State("In The Nether", "Nether", "nether"));
//        map.put("minecraft:the_end", new State("In The End", "The End", "end"));
//        map.put("yamda:mining_dim", new State("In YAMDA Mining Dim", "Mining Dim", "yamda"));
    }
}