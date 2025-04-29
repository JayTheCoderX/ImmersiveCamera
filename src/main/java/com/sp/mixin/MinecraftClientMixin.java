package com.sp.mixin;

import com.sp.SPBRevamped;
import com.sp.SPBRevampedClient;
import com.sp.networking.callbacks.ClientConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.resource.ResourcePackManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow private static MinecraftClient instance;

    @Shadow @Final public GameOptions options;

    @Shadow @Final private ResourcePackManager resourcePackManager;

    @ModifyArg(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;setPerspective(Lnet/minecraft/client/option/Perspective;)V"))
    private Perspective disableF5(Perspective perspective){
        return perspective;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, CallbackInfo ci){
        if(!(screen instanceof ProgressScreen)) {
            ClientConnectionEvents.DISCONNECT.invoker().onLoginDisconnect((MinecraftClient) (Object) this);
        }
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;render(Z)V", shift = At.Shift.AFTER))
    private void enableDeferredResourcePack(CallbackInfo ci){
        if (instance != null && resourcePackManager != null && false) {
            if(!resourcePackManager.getEnabledProfiles().contains(resourcePackManager.getProfile("veil:deferred"))) {
                SPBRevamped.LOGGER.info("Re-enabled Deferred Resourcepack");
                resourcePackManager.enable("veil:deferred");
                options.refreshResourcePacks(resourcePackManager);
            }
        }
    }
}
