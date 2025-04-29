package com.sp.mixin;

import com.sp.SPBRevampedClient;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (ClientPlayerEntity.class)
public class InterpolMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void cameraShakeCT(CallbackInfo ci) {
        if (SPBRevampedClient.shouldRenderCameraEffect()) {
            SPBRevampedClient.getCameraShake().ct();
        }
    }
}