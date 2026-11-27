package dev.nanomango.futureads.mixin;

import dev.nanomango.futureads.AdWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenAdsMixin extends Screen {
    protected MultiplayerScreenAdsMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void futureads$addAds(CallbackInfo ci) {
        AdWidget.install(this, true, this::addDrawableChild);
    }
}
