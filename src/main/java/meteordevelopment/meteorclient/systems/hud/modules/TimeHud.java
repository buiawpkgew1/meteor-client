/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.utils.Utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TimeHud extends DoubleTextHudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Type> timeType = sgGeneral.add(new EnumSetting.Builder<Type>()
        .name("类型")
        .description("使用哪种时间.")
        .defaultValue(Type.Game)
        .build()
    );

    public TimeHud(HUD hud) {
        super(hud, "时间", "显示世界时间.", "时间: ");
    }

    @Override
    protected String getRight() {
        return switch (timeType.get()) {
            case Game -> isInEditor() ? "00:00" : Utils.getWorldTime();
            case Local -> LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        };
    }

    public enum Type {
        Local,
        Game
    }
}
