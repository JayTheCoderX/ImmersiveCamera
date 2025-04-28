package com.sp.render.camera;

import com.sp.SPBRevampedClient;
import com.sp.compat.modmenu.ConfigStuff;
import com.sp.util.MathStuff;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

public class CameraShake {
    public double trauma;
    private double traumaGoal;
    public double noiseSpeed;
    private double noiseSpeedGoal;
    private double noiseY;
    private double playerOldY;
    private double playerY;
    private double amplitude;
    private PerlinNoiseSampler noiseSampler;
    private float cameraZRot;

    public CameraShake(){
        this.trauma = 0.1;
        this.noiseSpeed = 0.1;
        this.noiseY = 0;
        this.playerOldY = 0.0;
        this.amplitude = 5;
        this.noiseSampler = new PerlinNoiseSampler(Random.create());
        this.cameraZRot = 0.0f;
    }
    public void ct() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        this.playerOldY = playerY;
        this.playerY = player.getY();
    }

    public void tick(Camera camera) {
        if (ConfigStuff.enableRealCamera && !SPBRevampedClient.getCutsceneManager().isPlaying) {
            float frameDelta = MinecraftClient.getInstance().getLastFrameDuration();
            if (this.noiseY >= 1000) {
                this.noiseY = 0;
            }

            PlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                float playerSpeed = (player.horizontalSpeed - player.prevHorizontalSpeed) * 6;

                this.traumaGoal = MathHelper.clamp(0.6 * playerSpeed, 0.5, 1.5f);
                this.noiseSpeedGoal = MathHelper.clamp(0.25 * playerSpeed, 0.1, 1.0f);
                this.amplitude = 4;

                this.trauma = Math.max(MathStuff.Lerp((float) this.trauma, (float) this.traumaGoal, 0.93f, frameDelta), 0.5);
                this.noiseSpeed = Math.max(MathStuff.Lerp((float) this.noiseSpeed, (float) this.noiseSpeedGoal, 0.93f, frameDelta), 0.1);

                this.noiseY += ((this.noiseSpeed * frameDelta) * ((playerSpeed*0.6)+0.2));

                double pitchOffset = this.amplitude * this.getShakeIntensity() * (this.noiseSampler.sample(1, this.noiseY, 0) * (((playerOldY-playerY)*frameDelta*1000)));
                double yawOffset = this.amplitude * this.getShakeIntensity() * (this.noiseSampler.sample(73, this.noiseY, 0));
                double rollOffset = this.amplitude * this.getShakeIntensity() * (this.noiseSampler.sample(146, this.noiseY, 0));

                camera.setRotation((float) (camera.getYaw() + yawOffset), (float) (camera.getPitch() + pitchOffset));
                this.cameraZRot = (float) rollOffset * 2;
            }
        }
    }

    private double getShakeIntensity(){
        return this.trauma * this.trauma;
    }

    public float getCameraZRot() {
        return this.cameraZRot;
    }

}
