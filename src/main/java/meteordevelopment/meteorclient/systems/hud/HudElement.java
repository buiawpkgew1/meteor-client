/*
 * This file is part of the Meteor Client distribution (https://github.com/buiawpkgew1/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.other.Snapper;
import net.minecraft.nbt.NbtCompound;

public abstract class HudElement implements Snapper.Element, ISerializable<HudElement> {
    public final HudElementInfo<?> info;
    private boolean active;

    public final Settings settings = new Settings();
    public final HudBox box = new HudBox(this);

    public boolean autoAnchors = true;
    public int x, y;

    public HudElement(HudElementInfo<?> info) {
        this.info = info;
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        active = !active;
    }

    public void setSize(double width, double height) {
        box.setSize(width, height);
    }

    @Override
    public void setPos(int x, int y) {
        if (autoAnchors) {
            box.setPos(x, y);
            box.xAnchor = XAnchor.Left;
            box.yAnchor = YAnchor.Top;
            box.updateAnchors();
        }
        else {
            box.setPos(box.x + (x - this.x), box.y + (y - this.y));
        }

        updatePos();
    }

    @Override
    public void move(int deltaX, int deltaY) {
        box.move(deltaX, deltaY);
        updatePos();
    }

    public void updatePos() {
        x = box.getRenderX();
        y = box.getRenderY();
    }

    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth(), width, alignment);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return box.width;
    }

    @Override
    public int getHeight() {
        return box.height;
    }

    protected boolean isInEditor() {
        return !Utils.canUpdate() || HudEditorScreen.isOpen();
    }

    public void remove() {
        Hud.get().remove(this);
    }

    public void tick(HudRenderer renderer) {}

    public void render(HudRenderer renderer) {}

    public void onFontChanged() {}

    public WWidget getWidget(GuiTheme theme) {
        return null;
    }

    // Serialization

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("name", info.name);
        tag.putBoolean("active", active);

        tag.put("settings", settings.toTag());
        tag.put("box", box.toTag());

        tag.putBoolean("autoAnchors", autoAnchors);

        return tag;
    }

    @Override
    public HudElement fromTag(NbtCompound tag) {
        settings.reset();

        tag.getBoolean("active").ifPresent(active1 -> active = active1);

        settings.fromTag(tag.getCompoundOrEmpty("settings"));
        box.fromTag(tag.getCompoundOrEmpty("box"));

        tag.getBoolean("autoAnchors").ifPresent(autoAnchors1 -> autoAnchors = autoAnchors1);

        x = box.getRenderX();
        y = box.getRenderY();

        return this;
    }
}
