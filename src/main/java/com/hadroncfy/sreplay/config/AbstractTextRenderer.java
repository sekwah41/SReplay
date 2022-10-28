package com.hadroncfy.sreplay.config;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.awt.*;

public abstract class AbstractTextRenderer<C> {
    protected abstract MutableComponent renderString(String s);

    public Component render(C ctx, Component template) {
        MutableComponent ret;
        if (template instanceof TextComponent) {
            ret = renderString(((TextComponent) template).getText());
            // TODO check these TranslatableContents
        } else if (template instanceof TranslatableContents) {
            final TranslatableContents tc = (TranslatableContents) template;
            Object[] args = new Object[tc.getArgs().length];
            for (int i = 0; i < args.length; i++) {
                Object obj = tc.getArgs()[i];
                if (obj instanceof Component) {
                    obj = render(ctx, (Component) obj);
                } else {
                    obj = "null";
                }
                args[i] = obj;
            }
            ret = Component.translatable(tc.getKey(), args);
        } else {
            ret = template.plainCopy();
        }
        ret.setStyle(renderStyle(ctx, template.getStyle()));

        for (Component t : template.getSiblings()) {
            ret.append(render(ctx, t));
        }
        return ret;
    }

    private Style renderStyle(C ctx, /* mut */ Style style) {
        HoverEvent h = style.getHoverEvent();
        if (h != null && h.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Component action = h.getValue(HoverEvent.Action.SHOW_TEXT);
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, render(ctx, action)));
        }

        ClickEvent c = style.getClickEvent();
        if (c != null){
            style = style.withClickEvent(new ClickEvent(c.getAction(), renderString(c.getValue()).getContents().toString()));
        }

        String i = style.getInsertion();
        if (i != null){
            style = style.withInsertion(renderString(i).getContents().toString());
        }

        return style;
    }
}