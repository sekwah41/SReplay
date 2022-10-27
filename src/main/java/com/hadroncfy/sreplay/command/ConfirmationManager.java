package com.hadroncfy.sreplay.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import net.minecraft.commands.CommandSourceStack;
import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;

public class ConfirmationManager {
    private static final Random random = new Random();
    private final Map<String, ConfirmationEntry> confirms = new HashMap<>();
    private final long timeout;
    private final int codeBound;
    public ConfirmationManager(long timeout, int codeBound){
        this.timeout = timeout;
        this.codeBound = codeBound;
    }

    public synchronized void submit(String label, CommandSourceStack src, Runnable h){
        final int code = random.nextInt(codeBound);
        //src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().confirmingHint, Integer.toString(code)), false);
        ConfirmationEntry e = confirms.put(label, new ConfirmationEntry(src, label, code, h));
        if (e != null){
            e.t.cancel();
        }
        this.confirm(label, code);
    }

    public synchronized boolean confirm(String label, int code){
        ConfirmationEntry h = confirms.get(label);
        if (h != null){
            if (code == h.code){
                h.t.cancel();
                h.handler.run();
                confirms.remove(label);
            }
            else {
                h.src.sendFailure(SReplayMod.getFormats().incorrectConfirmationCode);
            }
            return true;
        }
        return false;
    }

    public synchronized boolean cancel(String label){
        ConfirmationEntry h = confirms.get(label);
        if (h != null){
            h.t.cancel();
            confirms.remove(label);
            h.src.sendSuccess(SReplayMod.getFormats().operationCancelled, true);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface ConfirmationHandler {
        void onConfirm(boolean codeMatch, boolean cancelled);
    }

    private class ConfirmationEntry extends TimerTask {
        final String label;
        final CommandSourceStack src;
        final Runnable handler;
        final Timer t;
        final int code;
        public ConfirmationEntry(CommandSourceStack src, String label, int code, Runnable h){
            this.src = src;
            this.label = label;
            this.handler = h;
            t = new Timer();
            this.code = code;
            t.schedule(this, timeout);
        }

        @Override
        public void run() {
            synchronized(ConfirmationManager.this){
                confirms.remove(label);
                src.sendSuccess(SReplayMod.getFormats().operationCancelled, true);
            }
        }
    }
}