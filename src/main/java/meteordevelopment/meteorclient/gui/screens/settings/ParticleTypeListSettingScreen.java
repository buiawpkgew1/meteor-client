/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;

import java.util.List;

public class ParticleTypeListSettingScreen extends LeftRightListSettingScreen<ParticleType<?>> {
    public ParticleTypeListSettingScreen(GuiTheme theme, Setting<List<ParticleType<?>>> setting) {
        super(theme, "Select Particles", setting, setting.get(), Registries.PARTICLE_TYPE);
    }

    @Override
    protected WWidget getValueWidget(ParticleType<?> value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(ParticleType<?> value) {
        return Names.get(value);
    }

    @Override
    protected boolean skipValue(ParticleType<?> value) {
        return !(value instanceof ParticleEffect);
    }
}
