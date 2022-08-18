/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;

public class SelfWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("自身网络使用的模式.")
            .defaultValue(Mode.Normal)
            .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("范围")
            .description("玩家必须在离你多远的地方才能放网要求模式为智能.")
            .defaultValue(3)
            .min(1)
            .sliderRange(1, 7)
            .visible(() -> mode.get() == Mode.Smart)
            .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("双位")
            .description("在你的上层攻击箱中也放置网状物.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
            .name("自动切换")
            .description("放置网络后切换为关闭.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("轮换")
            .description("在放置网络时，迫使你向下旋转.")
            .defaultValue(true)
            .build()
    );

    public SelfWeb() {
        super(Categories.Combat, "自网", "自动在你身上放置蜘网.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        switch (mode.get()) {
            case Normal:
                placeWeb();
                break;
            case Smart:
                if (TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance) != null) placeWeb();
                break;
        }
    }

    private void placeWeb() {
        FindItemResult web = InvUtils.findInHotbar(Items.COBWEB);

        BlockUtils.place(mc.player.getBlockPos(), web, rotate.get(), 0, false);

        if (doubles.get()) {
            BlockUtils.place(mc.player.getBlockPos().add(0, 1, 0), web, rotate.get(), 0, false);
        }

        if (turnOff.get()) toggle();
    }

    public enum Mode {
        Normal,
        Smart
    }
}
