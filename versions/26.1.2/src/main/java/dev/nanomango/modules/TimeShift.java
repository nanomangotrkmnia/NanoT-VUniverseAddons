package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class TimeShift extends Module {
    public enum PacketType {
        OnGroundOnly,
        PositionAndOnGround,
        LookAndOnGround,
        Full
    }

    private final SettingGroup sgHealth = settings.createGroup("Health");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> health = sgHealth.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Accelerate health regeneration below the trigger health.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> triggerHealth = sgHealth.add(new IntSetting.Builder()
        .name("health")
        .description("Health below which packets are sent.")
        .defaultValue(16)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> increment = sgHealth.add(new IntSetting.Builder()
        .name("increment")
        .description("Multiplier applied to the missing health.")
        .defaultValue(20)
        .range(0, 60)
        .build()
    );

    private final Setting<Boolean> badEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("bad-effects")
        .description("Shorten harmful status effects.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fire = sgGeneral.add(new BoolSetting.Builder()
        .name("fire")
        .description("Shorten fire ticks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maximumSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("maximum-speed")
        .description("Packet cap for bad-effects.")
        .defaultValue(100)
        .range(5, 200)
        .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
        .name("timer")
        .description("Client timer multiplier while packets are being sent.")
        .defaultValue(1)
        .range(0.1, 10)
        .build()
    );

    private final Setting<PacketType> packetType = sgGeneral.add(new EnumSetting.Builder<PacketType>()
        .name("packet-type")
        .description("Movement packet to spam.")
        .defaultValue(PacketType.Full)
        .build()
    );

    private final Setting<Boolean> pauseInAir = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-in-air")
        .description("Stop while airborne.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseWhenMoving = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-when-moving")
        .description("Stop while using movement keys.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseDuringRegeneration = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-during-regeneration")
        .description("Stop while the Regeneration effect is active.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseWhenHungry = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-when-hungry")
        .description("Stop unless the hunger bar is full.")
        .defaultValue(false)
        .build()
    );

    private boolean timerOverridden;

    public TimeShift() {
        super(NanoFutureAddon.CATEGORY, "time-shift",
            "Sends movement packet bursts to speed up regen and effects on 1.8 servers. Client 1.17+ is supported.",
            "zoot", "regen");
    }

    @Override
    public void onActivate() {
        if (!Utils.canUpdate()) return;

        ServerData server = mc.getCurrentServer();
        if (server == null || FabricLoader.getInstance().isModLoaded("viafabricplus")) return;

        if (server.protocol > 47) {
            warning("This server is not 1.8. TimeShift will not speed anything up.");
        }
    }

    @Override
    public void onDeactivate() {
        resetTimer();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || !mc.player.isAlive() || mc.player.getAbilities().instabuild || shouldPause()) {
            resetTimer();
            return;
        }

        int speed = 0;

        if (health.get()) {
            double missing = triggerHealth.get() - mc.player.getHealth();
            if (missing > 0) speed = Math.max(speed, (int) (missing * increment.get()));
        }

        if (fire.get() && mc.player.isOnFire() && !mc.player.isInWaterOrRain()) {
            speed = Math.max(speed, 9);
        }

        if (badEffects.get()) {
            for (MobEffectInstance effect : mc.player.getActiveEffects()) {
                if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) continue;

                int duration = effect.getDuration();
                if (duration <= 0) continue;

                speed = Math.max(speed, Math.min(duration / 20, maximumSpeed.get()));
            }
        }

        if (speed <= 0) {
            resetTimer();
            return;
        }

        Modules.get().get(Timer.class).setOverride(timer.get());
        timerOverridden = true;

        for (int i = 0; i < speed; i++) {
            mc.getConnection().send(createPacket());
        }
    }

    private void resetTimer() {
        if (!timerOverridden) return;

        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        timerOverridden = false;
    }

    private boolean shouldPause() {
        if (pauseInAir.get() && !mc.player.onGround()) return true;
        if (pauseWhenMoving.get() && isMoving()) return true;
        if (pauseDuringRegeneration.get() && mc.player.hasEffect(MobEffects.REGENERATION)) return true;
        return pauseWhenHungry.get() && mc.player.getFoodData().getFoodLevel() < 20;
    }

    private boolean isMoving() {
        return mc.options.keyUp.isDown()
            || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown()
            || mc.options.keyRight.isDown();
    }

    private ServerboundMovePlayerPacket createPacket() {
        boolean onGround = mc.player.onGround();
        boolean collision = mc.player.horizontalCollision;

        return switch (packetType.get()) {
            case OnGroundOnly -> new ServerboundMovePlayerPacket.StatusOnly(onGround, collision);
            case PositionAndOnGround -> new ServerboundMovePlayerPacket.Pos(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(), onGround, collision);
            case LookAndOnGround -> new ServerboundMovePlayerPacket.Rot(
                mc.player.getYRot(), mc.player.getXRot(), onGround, collision);
            case Full -> new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getYRot(), mc.player.getXRot(), onGround, collision);
        };
    }
}
