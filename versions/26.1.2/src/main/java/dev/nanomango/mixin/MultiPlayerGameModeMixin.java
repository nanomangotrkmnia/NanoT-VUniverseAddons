package dev.nanomango.mixin;

import dev.nanomango.modules.KeepBreakProgress;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStopDestroyBlock(CallbackInfo info) {
        Modules modules = Modules.get();
        if (modules == null) return;

        KeepBreakProgress keepBreakProgress = modules.get(KeepBreakProgress.class);
        if (keepBreakProgress != null && keepBreakProgress.shouldKeepBreaking()) info.cancel();
    }
}
