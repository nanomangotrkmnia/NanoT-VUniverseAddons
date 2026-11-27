package dev.nanomango.futureads.mixin;

import dev.nanomango.futureads.AdWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenAdsMixin extends Screen {
    protected JoinMultiplayerScreenAdsMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void futureads$addAds(CallbackInfo ci) {
        AdWidget.install(this, true, this::addRenderableWidget);
    }
}
