package dev.nanomango.mixin;

import dev.nanomango.ads.AdWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenAdsMixin extends Screen {
    protected TitleScreenAdsMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void nanofuture$addAds(CallbackInfo ci) {
        // The standalone FutureAds mod draws the same ads; don't show them twice.
        if (FabricLoader.getInstance().isModLoaded("futureads")) return;
        AdWidget.install(this, false, this::addDrawableChild);
    }
}
