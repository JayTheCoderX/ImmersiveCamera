package com.sp.clientWrapper;

import com.sp.Keybinds;
import com.sp.SPBRevampedClient;
import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.compat.modmenu.ConfigStuff;
import com.sp.init.ModSounds;
import com.sp.networking.InitializePackets;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.light.AreaLight;
import foundry.veil.api.client.render.deferred.light.PointLight;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.List;

/**
 * This class is just here to avoid dedicated server crashes.
 * Minecraft seams to crash even when a client class is present in a method without being called on the client.
 * This mostly happens with Sound Instances. And Veil lights.
 **/
public class ClientWrapper {

    public static void tickClientPlayerComponent(PlayerComponent playerComponent) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null && playerComponent.player == client.player) {
            SoundManager soundManager = client.getSoundManager();

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //Get a list of all the smilers in the area and see if any of them can see you
            //Update smiler glitch effect
            if (playerComponent.shouldGlitch()) {
                playerComponent.glitchTick = Math.min(playerComponent.glitchTick + 1, 80);
                playerComponent.glitchTimer = Math.min((float) playerComponent.glitchTick / 80, 1.0f);

                if (playerComponent.glitchTimer >= 0.25f) {
                    if (!playerComponent.shouldInflictGlitchDamage) {
                        playerComponent.shouldInflictGlitchDamage = true;
//                                System.out.println("SENT TRUE TO: " + playerComponent.player.getName().toString());
                        SPBRevampedClient.sendGlitchDamagePacket(true);
                    }
                }

            } else if (!playerComponent.isTeleportingToPoolrooms()) {
                playerComponent.glitchTick = Math.max(playerComponent.glitchTick - 1, 0);
                playerComponent.glitchTimer = Math.max((float) playerComponent.glitchTick / 80, 0.0f);

                if (playerComponent.glitchTimer <= 0) {
                    if (soundManager.isPlaying(playerComponent.GlitchAmbience)) {
                        soundManager.stop(playerComponent.GlitchAmbience);
                    }
                }

                if (playerComponent.glitchTimer <= 0.75f) {
                    if (playerComponent.shouldInflictGlitchDamage) {
                        playerComponent.shouldInflictGlitchDamage = false;
//                                System.out.println("SENT FALSE TO: " + playerComponent.player.getName().toString());
                        SPBRevampedClient.sendGlitchDamagePacket(false);
                    }
                }
            }

            //Teleporting to poolrooms Glitch
            if (playerComponent.isTeleportingToPoolrooms()) {
                playerComponent.glitchTick = Math.min(playerComponent.glitchTick + 1, 120);
                playerComponent.glitchTimer = Math.min((float) playerComponent.glitchTick / 120, 1.0f);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //Sync Target Entity for updating SkinWalker suspicion
            if (playerComponent.getTargetEntity() != client.targetedEntity) {
                playerComponent.setTargetEntity(client.targetedEntity);

                PacketByteBuf buffer = PacketByteBufs.create();
                if (playerComponent.getTargetEntity() != null) {
                    buffer.writeInt(playerComponent.getTargetEntity().getId());
                } else {
                    buffer.writeInt(-1);
                }
                ClientPlayNetworking.send(InitializePackets.TARGET_ENTITY_SYNC, buffer);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //Flavor text while being controlled by the SkinWalker

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //Client side stuff for level 0 -> 1 and 1 -> 2 transitions

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //Flashlight

            if (Keybinds.toggleFlashlight.wasPressed()) {
                playerComponent.player.playSound(ModSounds.FLASHLIGHT_CLICK, 0.5f, 1);
                if (true) {
                    playerComponent.setFlashLightOn(!playerComponent.isFlashLightOn());

                    if (!playerComponent.player.isSpectator()) {
                        PacketByteBuf buffer = PacketByteBufs.create();
                        buffer.writeBoolean(playerComponent.isFlashLightOn());
                        ClientPlayNetworking.send(InitializePackets.FL_SYNC, buffer);
                    }
                }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            ////AMBIENCE////
            RegistryKey<World> levelKey = playerComponent.player.getWorld().getRegistryKey();

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            //Level0 Cutscene
            if (playerComponent.player.isInsideWall() && playerComponent.player.getWorld().getRegistryKey() == World.OVERWORLD && !playerComponent.isDoingCutscene()) {
                playerComponent.suffocationTimer++;
                if (playerComponent.suffocationTimer >= 40) {
                    playerComponent.setDoingCutscene(true);
                    playerComponent.suffocationTimer = 0;
                }
            }

        }
    } 

    public static void doClientSideTick(World world, BlockPos pos, BlockState state) {
        if (!world.isClient) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        Vec3d position = pos.toCenterPos();

        if (player != null) {
        }
    }

}
