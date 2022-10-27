package com.hadroncfy.sreplay.recording.param;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public interface Validator<T> {
    boolean validate(T val, Consumer<Component> errorReceiver);
}