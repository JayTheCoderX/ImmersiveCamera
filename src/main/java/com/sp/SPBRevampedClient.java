package com.sp;

import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.compat.modmenu.ConfigDefinitions;
import com.sp.compat.modmenu.ConfigStuff;
import com.sp.init.*;
import com.sp.networking.InitializePackets;
import com.sp.networking.callbacks.ClientConnectionEvents;
import com.sp.render.*;
import com.sp.render.camera.CameraShake;
import com.sp.render.grass.GrassRenderer;
import com.sp.render.gui.StaminaBar;
import com.sp.render.pbr.PbrRegistry;
import com.sp.render.RenderLayers;
import com.sp.util.MathStuff;
import com.sp.util.TickTimer;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.deferred.VeilDeferredRenderer;
import foundry.veil.api.client.render.deferred.light.renderer.LightRenderer;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.definition.ShaderPreDefinitions;
import foundry.veil.api.client.render.shader.program.MutableUniformAccess;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.Vector;


public class SPBRevampedClient implements ClientModInitializer {
    private GrassRenderer grassRenderer;
    private static final CameraShake cameraShake = new CameraShake();
    private final FlashlightRenderer flashlightRenderer = new FlashlightRenderer();
    private static final Identifier VHS_POST = new Identifier(SPBRevamped.MOD_ID, "vhs");
    private static final Identifier SSAO = new Identifier(SPBRevamped.MOD_ID, "vhs/ssao");
    private static final Identifier EVERYTHING_SHADER = new Identifier(SPBRevamped.MOD_ID, "vhs/everything");
    private static final Identifier POST_VHS = new Identifier(SPBRevamped.MOD_ID, "vhs/vhs_post");
    private static final Identifier MIXED_SHADER = new Identifier(SPBRevamped.MOD_ID, "vhs/mixed");
    private static final Identifier GLITCH_SHADER = new Identifier(SPBRevamped.MOD_ID, "vhs/glitch");

    static boolean inBackrooms = false;
    public static Camera camera;

    public static TickTimer tickTimer = new TickTimer();
    public static boolean blackScreen;
    public static boolean youCantEscape;

    private static boolean shouldBeUnmuted = false;

    private static final Random random = Random.create();
    private static final Random random2 = Random.create(34563264);

    public static boolean shoudlRenderWarp = false;

    @Override
    public void onInitializeClient() {

        InitializePackets.registerS2CPackets();

        com.sp.Keybinds.initializeKeyBinds();

        VeilEventPlatform.INSTANCE.onVeilRenderTypeStageRender((stage, levelRenderer, bufferSource, poseStack, projectionMatrix, renderTick, partialTicks, camera, frustum) -> {
            //*Setting for later use
            if(camera != null){
                SPBRevampedClient.camera = camera;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            World clientWorld = client.world;

            //*Enable the VHS shader
            PostProcessingManager postProcessingManager = VeilRenderSystem.renderer().getPostProcessingManager();
            if (stage == Stage.AFTER_LEVEL) {
                //*Flashlight
                flashlightRenderer.renderFlashlightForEveryPlayer(partialTicks);

                if (client.player != null) {
                    if (clientWorld != null) {
                        PlayerComponent playerComponent = InitializeComponents.PLAYER.get(client.player);
                    }

                    if(!client.player.isSpectator() && !client.player.isCreative()){
                        //client.options.debugEnabled = false;
                    }

                }
            }

        });


        VeilEventPlatform.INSTANCE.preVeilPostProcessing(((name, pipeline, context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = MinecraftClient.getInstance().player;
            VeilRenderer renderer = VeilRenderSystem.renderer();
            ShaderPreDefinitions definitions = renderer.getShaderDefinitions();

            if (player != null && client.world != null) {
                PlayerComponent playerComponent = InitializeComponents.PLAYER.get(player);

                ConfigDefinitions.definitions.forEach((s, aBoolean) -> {
                    if(aBoolean.get()){
                        definitions.define(s);
                    } else {
                        definitions.remove(s);
                    }
                });

                PreviousUniforms.update();
            }
        }));

        ClientPlayConnectionEvents.JOIN.register(((handler,sender, client) -> {
            VeilDeferredRenderer renderer = VeilRenderSystem.renderer().getDeferredRenderer();
            renderer.reset();

            client.player.sendMessage(Text.translatable("flashlight.hint", com.sp.Keybinds.toggleFlashlight.getBoundKeyLocalizedText().copyContentOnly().formatted(Formatting.BOLD, Formatting.UNDERLINE)));

            //*Just in case it become unsynced
            if(client.world != null){
                    return;
            }

        }));

        //*For some reason veil lights aren't removed when you leave the game
        ClientConnectionEvents.DISCONNECT.register(client -> {
            PlayerEntity player = client.player;
            if (player != null) {
                PlayerComponent playerComponent = InitializeComponents.PLAYER.get(player);
                playerComponent.setFlashLightOn(false);
                flashlightRenderer.clearFlashlights();
                playerComponent.setDoingCutscene(false);
            }

        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {

        });


        ClientTickEvents.END_WORLD_TICK.register((client) ->{
            Vector<TickTimer> tickTimers = TickTimer.getAllInstances();
            if(!tickTimers.isEmpty()){
                for(TickTimer timer : tickTimers){
                    timer.addCurrentTick();
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register((client) ->{

            PlayerEntity playerClient = client.player;
            if(playerClient != null){
                //*Main Set in Backrooms
            }
        });

    }

    public static void setShadowUniforms(MutableUniformAccess access, World world) {
        Matrix4f level0ViewMat = ShadowMapRenderer.createShadowModelView(camera.getPos().x, camera.getPos().y, camera.getPos().z, true).peek().getPositionMatrix();
        Matrix4f viewMat = ShadowMapRenderer.createShadowModelView(camera.getPos().x, camera.getPos().y, camera.getPos().z, world, true).peek().getPositionMatrix();

        access.setMatrix("level0ViewMatrix", level0ViewMat);
        access.setMatrix("viewMatrix", viewMat);
        access.setMatrix("IShadowViewMatrix", viewMat.invert());

        access.setMatrix("orthographMatrix", ShadowMapRenderer.createProjMat());
    }

    public static float getWarpTimer(World world) {
        return 0;
    }

    public static void sendGlitchDamagePacket(boolean shouldDamage) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(shouldDamage);
        ClientPlayNetworking.send(InitializePackets.GLITCH_DAMAGE_SYNC, buffer);
    }

    public static boolean isInBackrooms() {
        return inBackrooms;
    }

    public static boolean shouldRenderCameraEffect() {
        return isInBackrooms() || ConfigStuff.enableVhsEffect;
    }

    public static void setInBackrooms(boolean inBackrooms) {
        SPBRevampedClient.inBackrooms = inBackrooms;
    }

    public static CameraShake getCameraShake() {
        return cameraShake;
    }
}