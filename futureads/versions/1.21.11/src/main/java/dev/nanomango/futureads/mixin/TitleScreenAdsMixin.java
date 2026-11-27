package dev.nanomango.futureads.mixin;

import dev.nanomango.futureads.AdWidget;
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
    private void futureads$addAds(CallbackInfo ci) {
        AdWidget.install(this, false, this::addDrawableChild);
    }
}
