package com.hadroncfy.sreplay.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.hadroncfy.sreplay.Util;
import com.hadroncfy.sreplay.Util.Replacer;

public class TextRenderer extends AbstractTextRenderer<TextRenderer> implements Replacer<String> {
    private List<Object> vars = new ArrayList<>();
    private static final Pattern VAL_EXP = Pattern.compile("\\$[0-9]");
    
    public TextRenderer var(Object ...vars){
        for (Object v: vars){
            this.vars.add(v);
        }
        return this;
    }

    public Component render0(Component t){
        return render(this, t);
    }

    @Override
    protected MutableComponent renderString(String s) {
        return Component.literal(Util.replaceAll(VAL_EXP, s, this));
    }

    @Override
    public String get(String a) {
        try {
            int i = Integer.parseInt(a.substring(1));
            if (i > 0 && i <= vars.size()){
                return vars.get(i - 1).toString();
            }
        }
        catch(NumberFormatException e){}
        return a;
    }

    public static Component render(Component template, Object ...vars){
        return new TextRenderer().var(vars).render0(template);
    }
}