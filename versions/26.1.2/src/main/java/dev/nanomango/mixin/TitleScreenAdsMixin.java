package dev.nanomango.mixin;

import dev.nanomango.ads.AdWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenAdsMixin extends Screen {
    protected TitleScreenAdsMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void nanofuture$addAds(CallbackInfo ci) {
        // The standalone FutureAds mod draws the same ads; don't show them twice.
        if (FabricLoader.getInstance().isModLoaded("futureads")) return;
        AdWidget.install(this, false, this::addRenderableWidget);
    }
}
