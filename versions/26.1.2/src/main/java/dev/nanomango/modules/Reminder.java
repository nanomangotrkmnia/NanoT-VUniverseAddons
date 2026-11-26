package dev.nanomango.modules;

import dev.nanomango.NanoFutureAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public class Reminder extends Module {
    public enum Unit {
        Minutes(60_000L),
        Hours(3_600_000L),
        Days(86_400_000L);

        private final long millis;

        Unit(long millis) {
            this.millis = millis;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("interval")
        .description("How often to remind you.")
        .defaultValue(30)
        .range(1, 999)
        .onChanged(value -> reschedule())
        .build()
    );

    private final Setting<Unit> unit = sgGeneral.add(new EnumSetting.Builder<Unit>()
        .name("unit")
        .description("Minutes, hours, or days.")
        .defaultValue(Unit.Minutes)
        .onChanged(value -> reschedule())
        .build()
    );

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("Text shown in the toast.")
        .defaultValue("Reminder")
        .build()
    );

    private long nextReminder;

    public Reminder() {
        super(NanoFutureAddon.CATEGORY, "reminder", "Shows a toast with your text at a set interval.");
    }

    @Override
    public void onActivate() {
        reschedule();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long now = System.currentTimeMillis();
        if (now < nextReminder) return;

        mc.getToastManager().addToast(new MeteorToast.Builder("Reminder")
            .text(message.get())
            .icon(Items.CLOCK)
            .build());

        nextReminder = now + intervalMillis();
    }

    private void reschedule() {
        if (!isActive()) return;
        nextReminder = System.currentTimeMillis() + intervalMillis();
    }

    private long intervalMillis() {
        return (long) interval.get() * unit.get().millis;
    }
}
