package com.sp.cca_stuff;

import com.sp.SPBRevamped;
import com.sp.clientWrapper.ClientWrapper;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModDamageTypes;
import com.sp.init.ModSounds;
import com.sp.mixininterfaces.ServerPlayNetworkSprint;
import com.sp.sounds.voicechat.BackroomsVoicechatPlugin;
import com.sp.world.levels.BackroomsLevel;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ClientTickingComponent;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.sp.SPBRevamped.SLOW_SPEED_MODIFIER;

@SuppressWarnings("DataFlowIssue")
public class PlayerComponent implements AutoSyncedComponent, ClientTickingComponent, ServerTickingComponent {
    public final PlayerEntity player;
    private final SimpleInventory playerSavedMainInventory = new SimpleInventory(36);
    private SimpleInventory playerSavedArmorInventory = new SimpleInventory(4);
    private SimpleInventory playerSavedOffhandInventory = new SimpleInventory(1);

    private int stamina;
    private boolean tired;

    private int scrollingInInventoryTime;

    private boolean flashLightOn;
    private boolean shouldRender;
    private boolean isDoingCutscene;
    private boolean playingGlitchSound;
    private boolean shouldNoClip;
    private boolean isTeleporting;
    private int teleportingTimer = 60;
    private boolean isTeleportingToPoolrooms;
    private boolean readyForLevel1;
    private boolean readyForLevel2;
    private boolean readyForPoolrooms;
    private ChunkPos currentTeleportChunkPos;

    public int suffocationTimer;
    private int level2Timer;
    private boolean shouldDoStatic;

    private boolean isBeingCaptured;
    public boolean hasBeenCaptured;
    private boolean isBeingReleased;
    private Entity targetEntity;
    private int skinWalkerLookDelay;
    private boolean shouldBeMuted;
    private boolean isSpeaking;
    private int speakingBuffer;
    private float prevSpeakingTime;
    private boolean visibleToEntity;
    private int visibilityTimer;
    private int visibilityTimerCooldown;
    private boolean talkingTooLoud;
    private int talkingTooLoudTimer;
    private GameMode prevGameMode;

    public MovingSoundInstance DeepAmbience;
    public MovingSoundInstance GasPipeAmbience;
    public MovingSoundInstance WaterPipeAmbience;
    public MovingSoundInstance WarpAmbience;
    public MovingSoundInstance PoolroomsNoonAmbience;
    public MovingSoundInstance PoolroomsSunsetAmbience;
    public MovingSoundInstance GlitchAmbience;
    public MovingSoundInstance SmilerAmbience;
    public MovingSoundInstance WindAmbience;

    private boolean canSeeActiveSkinWalker;
    private boolean prevFlashLightOn;

    public float glitchTimer;
    private boolean shouldGlitch;
    public int glitchTick;
    public boolean shouldInflictGlitchDamage;

    public PlayerComponent(PlayerEntity player){
        this.stamina = 300;
        this.tired = false;
        this.scrollingInInventoryTime = 0;
        this.player = player;
        this.flashLightOn = false;
        this.shouldRender = true;
        this.shouldNoClip = false;
        this.shouldDoStatic = false;

        this.isDoingCutscene = false;

        this.isTeleporting = false;

        this.isBeingCaptured = false;
        this.hasBeenCaptured = false;
        this.isBeingReleased = false;
        this.skinWalkerLookDelay = 60;
        this.shouldBeMuted = false;
        this.isSpeaking = false;
        this.speakingBuffer = 80;
        this.prevSpeakingTime = 0;
        this.visibleToEntity = false;
        this.visibilityTimer = 15;
        this.visibilityTimerCooldown = 0;

        this.talkingTooLoud = false;
        this.talkingTooLoudTimer = 20;

        this.suffocationTimer = 0;
        this.level2Timer = 200;

        this.canSeeActiveSkinWalker = false;

        this.glitchTimer = 0.0f;
        this.shouldGlitch = false;
        this.glitchTick = 0;
    }

    private void savePlayerInventory() {
        PlayerInventory inventory = this.player.getInventory();
        DefaultedList<ItemStack> mainInventory = inventory.main;
        DefaultedList<ItemStack> armorInventory = inventory.armor;
        DefaultedList<ItemStack> offHand = inventory.offHand;

        this.saveInventory(mainInventory, this.playerSavedMainInventory);
        this.saveInventory(armorInventory, this.playerSavedArmorInventory);
        this.saveInventory(offHand, this.playerSavedOffhandInventory);
    }

    public void loadPlayerSavedInventory() {
        PlayerInventory inventory = this.player.getInventory();
        DefaultedList<ItemStack> mainInventory = inventory.main;
        DefaultedList<ItemStack> armorInventory = inventory.armor;
        DefaultedList<ItemStack> offHand = inventory.offHand;

        this.loadInventory(mainInventory, this.playerSavedMainInventory);
        this.loadInventory(armorInventory, this.playerSavedArmorInventory);
        this.loadInventory(offHand, this.playerSavedOffhandInventory);

        this.playerSavedMainInventory.clear();
    }

    private void saveInventory(DefaultedList<ItemStack> inventory1, Inventory inventory2){
        for (int i = 0; i < inventory1.size(); i++) {
            ItemStack itemStack = inventory1.get(i);
            if (!itemStack.isEmpty()) {
                inventory2.setStack(i, itemStack);
            }
        }
    }

    private void loadInventory(DefaultedList<ItemStack> inventory1, Inventory inventory2){
        for (int i = 0; i < inventory2.size(); i++) {
            ItemStack itemStack = inventory2.getStack(i);
            if (!itemStack.isEmpty()) {
                inventory1.set(i, itemStack);
            }
        }
    }

    public int getTeleportingTimer() {
        return teleportingTimer;
    }

    public void setTeleportingTimer(int teleportingTimer) {
        this.teleportingTimer = teleportingTimer;
    }

    public int getStamina() {
        return stamina;
    }
    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public boolean isTired() {
        return tired;
    }
    public void setTired(boolean tired) {
        this.tired = tired;
    }

    public int getScrollingInInventoryTime() {
        return scrollingInInventoryTime;
    }
    public void setScrollingInInventoryTime(int scrollingInInventoryTime) {
        this.scrollingInInventoryTime = scrollingInInventoryTime;
    }

    public boolean isShouldRender() {
        return shouldRender;
    }
    public void setShouldRender(boolean shouldRender) {
        this.shouldRender = shouldRender;
    }

    public void setFlashLightOn(boolean set){
        this.flashLightOn = set;
    }
    public boolean isFlashLightOn() {
        return flashLightOn;
    }

    public boolean isDoingCutscene() {
        return isDoingCutscene;
    }
    public void setDoingCutscene(boolean doingCutscene) {
        isDoingCutscene = doingCutscene;
    }

    public boolean isTeleporting() {
        return isTeleporting;
    }
    public void setTeleporting(boolean teleporting) {
        isTeleporting = teleporting;
    }

    public void setReadyForLevel1(boolean readyForLevel1) {
        this.readyForLevel1 = readyForLevel1;
    }

    public void setReadyForLevel2(boolean readyForLevel2) {
        this.readyForLevel2 = readyForLevel2;
    }

    public void setReadyForPoolrooms(boolean readyForPoolrooms) {
        this.readyForPoolrooms = readyForPoolrooms;
    }

    public boolean isTeleportingToPoolrooms() {
        return isTeleportingToPoolrooms;
    }
    public void setTeleportingToPoolrooms(boolean teleportingToPoolrooms) {
        isTeleportingToPoolrooms = teleportingToPoolrooms;
    }

    public boolean shouldNoClip() {
        return shouldNoClip;
    }
    public void setShouldNoClip(boolean shouldNoClip) {
        this.shouldNoClip = shouldNoClip;
    }

    public boolean isShouldDoStatic() {
        return shouldDoStatic;
    }
    public void setShouldDoStatic(boolean shouldDoStatic) {
        this.shouldDoStatic = shouldDoStatic;
    }

    public boolean isBeingCaptured() {return isBeingCaptured;}
    public void setBeingCaptured(boolean beingCaptured) {isBeingCaptured = beingCaptured;}

    public boolean hasBeenCaptured() {return hasBeenCaptured;}
    public void setHasBeenCaptured(boolean hasBeenCaptured) {this.hasBeenCaptured = hasBeenCaptured;}

    public boolean isBeingReleased() {
        return isBeingReleased;
    }
    public void setBeingReleased(boolean beingReleased) {
        isBeingReleased = beingReleased;
    }

    public Entity getTargetEntity() {return targetEntity;}
    public void setTargetEntity(Entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public int getSkinWalkerLookDelay() {
        return skinWalkerLookDelay;
    }
    public void setSkinWalkerLookDelay(int skinWalkerLookDelay) {
        this.skinWalkerLookDelay = skinWalkerLookDelay;
    }
    public void subtractSkinWalkerLookDelay() {
        this.skinWalkerLookDelay -= 1;
    }

    public boolean shouldBeMuted() {return shouldBeMuted;}
    public void setShouldBeMuted(boolean shouldStayUnmuted) {this.shouldBeMuted = shouldStayUnmuted;}

    public boolean isSpeaking() {
        return isSpeaking;
    }
    public void setSpeaking(boolean speaking) {
        isSpeaking = speaking;
    }

    public boolean isVisibleToEntity() {return visibleToEntity;}
    public void setVisibleToEntity(boolean visibleToEntity) {this.visibleToEntity = visibleToEntity;}

    public boolean isTalkingTooLoud() {
        return talkingTooLoud;
    }
    public void setTalkingTooLoud(boolean talkingTooLoud) {
        this.talkingTooLoud = talkingTooLoud;
    }

    public boolean isReadyForLevel1() {
        return readyForLevel1;
    }

    public boolean isReadyForLevel2() {
        return readyForLevel2;
    }

    public boolean isReadyForPoolrooms() {
        return readyForPoolrooms;
    }

    public void resetTalkingTooLoudTimer(){
        this.talkingTooLoudTimer = 20;
    }

    public GameMode getPrevGameMode() {
        return prevGameMode;
    }
    public void setPrevGameMode(GameMode prevGameMode) {
        this.prevGameMode = prevGameMode;
    }

    public boolean canSeeActiveSkinWalkerTarget() {return canSeeActiveSkinWalker;}
    public void setCanSeeActiveSkinWalkerTarget(boolean canSeeActiveSkinWalker) {this.canSeeActiveSkinWalker = canSeeActiveSkinWalker;}

    public float getGlitchTimer() {
        return glitchTimer;
    }

    public boolean shouldGlitch() {
        return shouldGlitch;
    }
    public void setShouldGlitch(boolean shouldGlitch) {
        this.shouldGlitch = shouldGlitch;
    }

    public void setShouldInflictGlitchDamage(boolean shouldInflictGlitchDamage) {
        this.shouldInflictGlitchDamage = shouldInflictGlitchDamage;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        this.stamina = tag.getInt("stamina");
        this.flashLightOn = tag.getBoolean("flashLightOn");
        this.shouldRender = tag.getBoolean("shouldRender");
        this.isDoingCutscene = tag.getBoolean("isDoingCutscene");
        this.playingGlitchSound = tag.getBoolean("playingGlitchSound");
        this.isTeleporting = tag.getBoolean("isTeleporting");
        this.isTeleportingToPoolrooms = tag.getBoolean("isTeleportingToPoolrooms");
        this.shouldNoClip = tag.getBoolean("shouldNoClip");
        this.shouldDoStatic = tag.getBoolean("shouldDoStatic");
        this.isBeingCaptured = tag.getBoolean("isBeingCaptured");
        this.hasBeenCaptured = tag.getBoolean("hasBeenCaptured");
        this.isBeingReleased = tag.getBoolean("isBeingReleased");
        this.shouldBeMuted = tag.getBoolean("shouldBeMuted");
        this.shouldGlitch = tag.getBoolean("shouldGlitch");
        this.shouldInflictGlitchDamage = tag.getBoolean("shouldInflictGlitchDamage");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putInt("stamina", this.stamina);
        tag.putBoolean("flashLightOn", this.flashLightOn);
        tag.putBoolean("shouldRender", this.shouldRender);
        tag.putBoolean("isDoingCutscene", this.isDoingCutscene);
        tag.putBoolean("playingGlitchSound", this.playingGlitchSound);
        tag.putBoolean("isTeleporting", this.isTeleporting);
        tag.putBoolean("isTeleportingToPoolrooms", this.isTeleportingToPoolrooms);
        tag.putBoolean("shouldNoClip", this.shouldNoClip);
        tag.putBoolean("shouldDoStatic", this.shouldDoStatic);
        tag.putBoolean("isBeingCaptured", this.isBeingCaptured);
        tag.putBoolean("hasBeenCaptured", this.hasBeenCaptured);
        tag.putBoolean("isBeingReleased", this.isBeingReleased);
        tag.putBoolean("shouldBeMuted", this.shouldBeMuted);
        tag.putBoolean("shouldGlitch", this.shouldGlitch);
        tag.putBoolean("shouldInflictGlitchDamage", this.shouldInflictGlitchDamage);
    }

    public void sync(){
        InitializeComponents.PLAYER.sync(this.player);
    }

    @Override
    public void clientTick() {
        ClientWrapper.tickClientPlayerComponent(this);
    }

    @Override
    public void serverTick() {
        getPrevSettings();

        //*Update Stamina
        updateStamina();

        //*Damage if glitched enough from smilers
        if(this.shouldInflictGlitchDamage){
            this.player.damage(ModDamageTypes.of(this.player.getWorld(), ModDamageTypes.SMILER), 1.0f);
        }

        //*Is speaking
        if(BackroomsVoicechatPlugin.speakingTime.containsKey(this.player.getUuid()) && BackroomsVoicechatPlugin.speakingTime.get(this.player.getUuid()) == this.prevSpeakingTime) {
            if(this.isSpeaking()) {
                this.speakingBuffer--;
                if (this.speakingBuffer <= 0) {
                    this.setSpeaking(false);
                    this.speakingBuffer = 80;
                }
            }
        } else {
            this.speakingBuffer = 80;
        }

        //*Cast him to the Backrooms
        if (checkBackroomsTeleport()) return;

        BackroomsLevel level = BackroomsLevels.getLevel(this.player.getWorld());

        if (level != null) {
            List<BackroomsLevel.LevelTransition> teleports = level.checkForTransition(this, this.player.getWorld());

            if (!teleports.isEmpty()) {
                for (BackroomsLevel.CrossDimensionTeleport crossDimensionTeleport : teleports.get(0).predicate(this.player.getWorld(), this, BackroomsLevels.getLevel(this.player.getWorld()))) {
                    if (crossDimensionTeleport.from().transitionOut(crossDimensionTeleport.to(), this, crossDimensionTeleport.world())) {
                        if (teleportingTimer == -1) {
                            teleportingTimer = 20;
                        }

                        if (teleportingTimer == 0) {
                            TeleportTarget target = new TeleportTarget(crossDimensionTeleport.pos(), crossDimensionTeleport.playerComponent().player.getVelocity(), crossDimensionTeleport.playerComponent().player.getYaw(), crossDimensionTeleport.playerComponent().player.getPitch());
                            FabricDimensions.teleport(crossDimensionTeleport.playerComponent().player, crossDimensionTeleport.world().getServer().getWorld(crossDimensionTeleport.to().getWorldKey()), target);
                        }
                    }
                }
            }
        }

        if (teleportingTimer >= 0) {
            teleportingTimer--;
        }


        /*
        //*Level 0 -> Level 1
        //*Level 1 -> Level 2
        checkLevel2Teleport();

        //*Level 2 -> Poolrooms
        checkPoolroomsTeleport();

        //*Poolrooms -> Grass Field
        checkGrassFieldsTeleport();

        //*Grass Field -> OverWorld
        checkOverWroldReturnTeleport();
        */

        //*Update Entity Visibility
        updateEntityVisibility();

        if(BackroomsVoicechatPlugin.speakingTime.containsKey(this.player.getUuid())) {
            this.prevSpeakingTime = BackroomsVoicechatPlugin.speakingTime.get(this.player.getUuid());
        }
        
        shouldSync();
    }

    private void updateStamina() {
        EntityAttributeInstance attributeInstance = this.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if(attributeInstance != null && false) {
            int prevStamina = this.stamina;
            if(!this.player.isCreative() && !this.player.isSpectator()){
                if(this.player.isSprinting()) {
                    this.stamina--;
                } else {
                    this.stamina = Math.min(this.stamina + 1, 300);
                }
            } else {
                this.stamina = 300;
            }

            if(this.stamina <= 0){
                this.stamina = 0;
                this.setTired(true);
            }

            if(this.isTired()){
                this.player.setSprinting(false);
                this.player.addExhaustion(0.05f);
                if(this.stamina > 200){
                    this.setTired(false);
                }
            } else if(!((ServerPlayNetworkSprint)((ServerPlayerEntity)this.player).networkHandler).getShouldStopSprinting()){
                this.player.setSprinting(true);
            }

            if ((!player.isSneaking() && !player.isSprinting()) || this.isTired()) {
                if(!attributeInstance.hasModifier(SLOW_SPEED_MODIFIER)) {
                    attributeInstance.addTemporaryModifier(SLOW_SPEED_MODIFIER);
                }
            } else if(attributeInstance.hasModifier(SLOW_SPEED_MODIFIER)) {
                attributeInstance.removeModifier(SLOW_SPEED_MODIFIER);
            }
            //*Mod by 20 to reduce packet count
            if(prevStamina != this.stamina && this.stamina % 20 == 0){
                //*Only sync with the specific player since other players don't need to know your stamina
                InitializeComponents.PLAYER.syncWith((ServerPlayerEntity) this.player, (ComponentProvider) this.player);
            }

        }
    }

    private boolean checkBackroomsTeleport() {
        return false;
    }

    /*
    private void checkLevel1Teleport() {
        if (this.player.getWorld().getRegistryKey() == BackroomsLevels.LEVEL0_WORLD_KEY) {
            ServerWorld level1 = this.player.getWorld().getServer().getWorld(BackroomsLevels.LEVEL1_WORLD_KEY);

            if (this.readyForLevel1) {
                this.currentTeleportChunkPos = this.player.getChunkPos();
                for (PlayerEntity players : this.player.getServer().getPlayerManager().getPlayerList()) {
                    if(players.getWorld().getRegistryKey() == BackroomsLevels.LEVEL0_WORLD_KEY) {
                        PlayerComponent playerComponent = InitializeComponents.PLAYER.get(players);
                        playerComponent.setReadyForLevel1(false);

                        TeleportTarget target = new TeleportTarget(calculateLevel1TeleportCoords(players), players.getVelocity(), players.getYaw(), players.getPitch());
                        FabricDimensions.teleport(players, level1, target);
                    }
                }
                this.currentTeleportChunkPos = null;
            }

            if (this.player.getPos().getY() <= 11 && this.player.isOnGround() && this.player.getWorld().getRegistryKey() == BackroomsLevels.LEVEL0_WORLD_KEY) {
                if (!this.isTeleporting() && !this.readyForLevel1) {
                    for (PlayerEntity players : this.player.getServer().getPlayerManager().getPlayerList()) {
                        if (players.getWorld().getRegistryKey() == BackroomsLevels.LEVEL0_WORLD_KEY) {
                            teleportPlayer(players, 1);
                        }
                    }
                }
            }
        }
    }
    */

    private void checkLevel2Teleport() {
        if (this.player.getWorld().getRegistryKey() == BackroomsLevels.LEVEL1_WORLD_KEY) {
            ServerWorld level2 = this.player.getWorld().getServer().getWorld(BackroomsLevels.LEVEL2_WORLD_KEY);

            if (this.readyForLevel2) {
                this.currentTeleportChunkPos = this.player.getChunkPos();
                for (PlayerEntity players : this.player.getServer().getPlayerManager().getPlayerList()) {
                    if(players.getWorld().getRegistryKey() == BackroomsLevels.LEVEL1_WORLD_KEY) {
                        PlayerComponent playerComponent = InitializeComponents.PLAYER.get(players);
                        playerComponent.setReadyForLevel1(false);

                        TeleportTarget target = new TeleportTarget(calculateLevel2TeleportCoords(players), players.getVelocity(), players.getYaw(), players.getPitch());
                        FabricDimensions.teleport(players, level2, target);
                    }
                }
                this.currentTeleportChunkPos = null;
            }

            if (this.player.getPos().getY() <= 12.5 && this.player.isOnGround()) {
                if (!this.isTeleporting() && !this.readyForLevel2) {
                    for (PlayerEntity players : this.player.getServer().getPlayerManager().getPlayerList()) {
                        if (players.getWorld().getRegistryKey() == BackroomsLevels.LEVEL1_WORLD_KEY) {
                            teleportPlayer(players, 2);
                        }
                    }
                }
            }
        }
    }

    private void checkPoolroomsTeleport() {
        if (this.player.getWorld().getRegistryKey() == BackroomsLevels.LEVEL2_WORLD_KEY) {
            ServerWorld poolrooms = this.player.getWorld().getServer().getWorld(BackroomsLevels.POOLROOMS_WORLD_KEY);

            if (Math.abs(this.player.getPos().getZ()) >= 1000) {
                this.level2Timer--;
                if(level2Timer <= 0){
                    if (!this.isTeleporting() && !this.readyForPoolrooms) {
                        startLevel2Teleport(this.player);
                    }
                    if (this.readyForPoolrooms) {
                        if(this.player.getWorld().getRegistryKey() == BackroomsLevels.LEVEL2_WORLD_KEY) {
                            TeleportTarget target = new TeleportTarget(new Vec3d(16, 106, 16), Vec3d.ZERO, this.player.getYaw(), this.player.getPitch());
                            FabricDimensions.teleport(this.player, poolrooms, target);
                        }
                        this.readyForPoolrooms = false;
                    }
                }
            } else {
                this.level2Timer = 200;
                this.readyForPoolrooms = false;
            }
        }
    }

    private void checkGrassFieldsTeleport() {
        if(this.player.getWorld().getRegistryKey() == BackroomsLevels.POOLROOMS_WORLD_KEY && this.player.getWorld().getLightLevel(this.player.getBlockPos()) == 0 && this.player.getPos().y < 60 && this.player.getPos().y > 52){
            this.player.fallDistance = 0;
            if(this.player instanceof ServerPlayerEntity) {
                SPBRevamped.sendBlackScreenPacket((ServerPlayerEntity) this.player, 60, true, false);
                ServerWorld grassField = this.player.getWorld().getServer().getWorld(BackroomsLevels.INFINITE_FIELD_WORLD_KEY);
                TeleportTarget target = new TeleportTarget(new Vec3d(0, 32, 0), Vec3d.ZERO, this.player.getYaw(), this.player.getPitch());
                FabricDimensions.teleport(this.player, grassField, target);
            }
        }
    }

    private void checkOverWroldReturnTeleport() {
        if(this.player.getWorld().getRegistryKey() == BackroomsLevels.INFINITE_FIELD_WORLD_KEY && this.player.getPos().y > 57.5 && this.player.isOnGround()){
            if(this.player instanceof ServerPlayerEntity) {
                BlockPos blockPos = ((ServerPlayerEntity)this.player).getSpawnPointPosition();
                float f = ((ServerPlayerEntity)this.player).getSpawnAngle();
                boolean bl = ((ServerPlayerEntity)this.player).isSpawnForced();
                ServerWorld serverWorld = this.player.getWorld().getServer().getWorld(World.OVERWORLD);
                Optional<Vec3d> optional;
                if (serverWorld != null && blockPos != null) {
                    optional = PlayerEntity.findRespawnPosition(serverWorld, blockPos, f, bl, true);
                } else {
                    optional = Optional.empty();
                }

                SPBRevamped.sendBlackScreenPacket((ServerPlayerEntity) this.player, 60, true, false);
                ServerWorld overworld = this.player.getWorld().getServer().getWorld(World.OVERWORLD);
                BlockPos blockPos1 = overworld.getSpawnPos();
                TeleportTarget target = new TeleportTarget(optional.orElseGet(() -> Vec3d.of(blockPos1)), Vec3d.ZERO, this.player.getYaw(), this.player.getPitch());
                this.loadPlayerSavedInventory();
                FabricDimensions.teleport(this.player, overworld, target);
            }
        }
    }

    private void updateEntityVisibility() {
        if(this.isTalkingTooLoud() && this.talkingTooLoudTimer >= 0){
            this.setVisibleToEntity(true);
            this.talkingTooLoudTimer--;
        } else if(this.talkingTooLoudTimer <= 0){
            this.setVisibleToEntity(false);
            this.setTalkingTooLoud(false);
            this.resetTalkingTooLoudTimer();
        }

        if(!this.isVisibleToEntity()) {
            float speed = this.player.horizontalSpeed - this.player.prevHorizontalSpeed;

            if (!this.player.isSneaking() && speed != 0) {
                this.visibilityTimer--;
            }
            if (this.player.isSprinting()) {
                this.visibilityTimer--;
            }

            //reset timer if the player is not moving
            if(speed == 0){
                this.visibilityTimer = 15;
            }

            if (this.visibilityTimer <= 0) {
                this.visibilityTimerCooldown = 20;
                this.setVisibleToEntity(true);
            }
        } else {
            if(this.visibilityTimerCooldown > 0){
                this.visibilityTimerCooldown--;
            } else {
                this.visibilityTimer = 15;
                if(!this.isTalkingTooLoud()) {
                    this.setVisibleToEntity(false);
                }
            }
        }
    }


    private void shouldSync() {
        boolean sync = false;

        if(this.prevFlashLightOn != this.flashLightOn){
            sync = true;
        }

        if(sync){
            this.sync();
        }
    }

    private void getPrevSettings() {
        this.prevFlashLightOn = this.flashLightOn;
    }

    private void teleportPlayer(PlayerEntity players, int destLevel){
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        PlayerComponent playerComponent = InitializeComponents.PLAYER.get(players);

        if(!playerComponent.isTeleporting()) {
            playerComponent.setTeleporting(true);
            playerComponent.sync();

            executorService.schedule(() -> {
                switch (destLevel){
                    case 1: playerComponent.setReadyForLevel1(true); break;
                    case 2: playerComponent.setReadyForLevel2(true); break;
                }
                playerComponent.setTeleporting(false);
                executorService.shutdown();
            }, 2500, TimeUnit.MILLISECONDS);
        }
    }

    private void startLevel2Teleport(PlayerEntity players){
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        PlayerComponent playerComponent = InitializeComponents.PLAYER.get(players);

        if(!playerComponent.isTeleportingToPoolrooms()) {
            //First send the signal to start the camera Shake
            playerComponent.setTeleportingToPoolrooms(true);
            playerComponent.sync();

            //Shake the Camera of each player
//            SPBRevamped.sendCameraShakePacket((ServerPlayerEntity) players, 100, 1, Easings.Easing.linear, true);

            //Then Noclip
            executorService.schedule(() -> {
                playerComponent.setShouldNoClip(true);
                playerComponent.sync();
                executorService.shutdown();
            }, 4500, TimeUnit.MILLISECONDS);

            //Turn Player screen to Black
            executorService.schedule(() -> {
                SPBRevamped.sendBlackScreenPacket((ServerPlayerEntity) players, 20, true, false);
                executorService.shutdown();
            }, 4800, TimeUnit.MILLISECONDS);

            //After the screen turns black THEN teleport
            executorService.schedule(() -> {
                this.readyForPoolrooms = true;
                playerComponent.setTeleportingToPoolrooms(false);
                playerComponent.setShouldNoClip(false);
                playerComponent.sync();
                executorService.shutdown();
            }, 5500, TimeUnit.MILLISECONDS);

        }
    }

    private Vec3d calculateLevel2TeleportCoords(PlayerEntity player){
        if(this.player.getChunkPos().equals(player.getChunkPos())) {
            int chunkX = this.currentTeleportChunkPos.getStartX();
            int chunkZ = this.currentTeleportChunkPos.getStartZ();

            double playerX = player.getPos().x;
            double playerZ = player.getPos().z;

            return new Vec3d((playerX - chunkX) - 1, player.getPos().y + 8, playerZ - chunkZ);
        } else {
            return new Vec3d(11.5, 29, 7.5);
        }
    }
}
