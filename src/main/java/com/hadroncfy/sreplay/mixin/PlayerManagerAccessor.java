package com.hadroncfy.sreplay.mixin;

import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerList.class)
public interface PlayerManagerAccessor {
    @Accessor("saveHandler")
    PlayerDataStorage getSaveHandler();
}